package fullstack2;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/doctor")
public class DoctorResource {

    @Inject
    @Named("auth")
    MySQLPool authPool;

    @Inject
    @Named("logs")
    MySQLPool logsPool;

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm")
    String realm;

    @GET
    @Path("/getByDoctorName/{doctorName}")
    //@Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getDoctorAndPatientsByDoctorName(@PathParam("doctorName") String doctorName) {

        return Uni.createFrom().item(() -> {

            // 1) Find doctor by username
            List<UserRepresentation> doctors = keycloak.realm(realm)
                    .users()
                    .searchByUsername(doctorName, true);

            if (doctors.isEmpty()) {
                // Return empty list (or throw 404 if you prefer)
                return List.<JsonObject>of();
            }

            UserRepresentation doctor = keycloak.realm(realm)
                    .users()
                    .get(doctors.get(0).getId())
                    .toRepresentation();

            Map<String, List<String>> dAttrs = doctor.getAttributes();

            String doctorUsername = doctor.getUsername();
            String doctorEmail = doctor.getEmail();

            // Organization info stored as attributes on doctor (adjust keys to your real ones)
            String orgName = getAttr(dAttrs, "organizationName");
            String orgAddress = getAttr(dAttrs, "organizationAddress");

            // 2) Doctor has "patients" attribute = list of patient usernames
            List<String> patientUsernames = dAttrs != null ? dAttrs.get("patients") : null;
            if (patientUsernames == null) patientUsernames = List.of();

            // 3) Build response: one item per patient
            List<JsonObject> result = new ArrayList<>();

            for (String patientUsername : patientUsernames) {
                if (patientUsername == null || patientUsername.isBlank()) continue;

                List<UserRepresentation> matches = keycloak.realm(realm)
                        .users()
                        .searchByUsername(patientUsername, true);

                if (matches.isEmpty()) {
                    // Patient username listed but no such Keycloak user exists -> skip
                    continue;
                }

                UserRepresentation patient = keycloak.realm(realm)
                        .users()
                        .get(matches.get(0).getId())
                        .toRepresentation();

                Map<String, List<String>> pAttrs = patient.getAttributes();

                JsonObject json = new JsonObject()
                        .put("doctor_name", doctorUsername)
                        .put("doctor_mail", doctorEmail)
                        .put("organization_name", orgName)
                        .put("organization_address", orgAddress)
                        .put("patient_name", patient.getUsername())
                        .put("patient_mail", patient.getEmail())
                        .put("patient_age", getAttr(pAttrs, "age"))
                        .put("patient_gender", getAttr(pAttrs, "gender"))
                        .put("patient_address", getAttr(pAttrs, "address"));

                result.add(json);
            }

            return result;
        });
    }

    private String getAttr(Map<String, List<String>> attrs, String key) {
        if (attrs != null && attrs.containsKey(key)) {
            List<String> values = attrs.get(key);
            if (values != null && !values.isEmpty()) {
                return values.get(0);
            }
        }
        return null;
    }

    @GET
    @Path("/getDoctorEncountersByDate/{doctorName}/{date}")
    //@Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getDoctorEncountersByDate(
            @PathParam("doctorName") String doctorName,
            @PathParam("date") String dateStr
    ) {
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr); // e.g. 2025-11-27
        } catch (Exception e) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Invalid date format, expected 2025-11-27")
            );
        }

        // 1) Resolve doctor user in Keycloak (by username)
        return Uni.createFrom().item(() -> {
            List<UserRepresentation> doctors = keycloak.realm(realm)
                    .users()
                    .searchByUsername(doctorName, true);

            if (doctors.isEmpty()) {
                return (UserRepresentation) null;
            }

            return keycloak.realm(realm)
                    .users()
                    .get(doctors.get(0).getId())
                    .toRepresentation();
        }).onItem().transformToUni(doctorUser -> {

            if (doctorUser == null) {
                return Uni.createFrom().item(List.<JsonObject>of());
            }

            String doctorId = doctorUser.getId();           // Keycloak ID (UUID)
            String doctorUsername = doctorUser.getUsername();

            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            // 2) Fetch encounters for that doctor within the date range
            String sql =
                    "SELECT e.id, e.date_time, e.doctor_id, e.patient_id, o.note " +
                            "FROM encounter e " +
                            "LEFT JOIN observation o ON e.id = o.encounter_id " +
                            "WHERE e.date_time >= ? AND e.date_time < ? " +
                            "  AND e.doctor_id = ?";

            return logsPool
                    .preparedQuery(sql)
                    .execute(Tuple.of(start, end, doctorId))
                    .onItem().transformToUni(rows -> {

                        List<Row> encounterRows = rows.stream().toList();
                        if (encounterRows.isEmpty()) {
                            return Uni.createFrom().item(List.<JsonObject>of());
                        }

                        // 3) Collect unique patient ids from encounters
                        List<String> patientIds = new ArrayList<>();
                        for (Row r : encounterRows) {
                            String pid = r.getString("patient_id"); // UUID string
                            if (pid != null && !patientIds.contains(pid)) {
                                patientIds.add(pid);
                            }
                        }

                        // 4) Resolve patient names from Keycloak
                        Map<String, String> patientNames = new HashMap<>();
                        for (String pid : patientIds) {
                            try {
                                UserRepresentation p = keycloak.realm(realm)
                                        .users()
                                        .get(pid)
                                        .toRepresentation();

                                patientNames.put(pid, p.getUsername());
                            } catch (Exception ignored) {
                                // patient id exists in DB but not in Keycloak -> name stays null
                            }
                        }

                        // 5) Build response
                        List<JsonObject> result = new ArrayList<>();
                        for (Row er : encounterRows) {
                            String pid = er.getString("patient_id");
                            String patientName = pid != null ? patientNames.get(pid) : null;

                            JsonObject json = new JsonObject()
                                    .put("encounter_id", er.getLong("id"))
                                    .put("date_time", er.getLocalDateTime("date_time").toString())
                                    .put("doctor_name", doctorUsername)
                                    .put("patient_name", patientName)
                                    .put("note", er.getString("note"));

                            result.add(json);
                        }

                        return Uni.createFrom().item(result);
                    });
        });
    }

}
