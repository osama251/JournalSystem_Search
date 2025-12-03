package fullstack2;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.Tuple;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

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

    @GET
    @Path("/getByDoctorName/{doctorName}")
    public Uni<List<JsonObject>> getDoctorAndPatientsByDoctorName(@PathParam("doctorName") String doctorName){
        String sql = "SELECT u.user_name as doctor_name, u.email as doctor_mail, o.address as organization_address, o.name as organization_name, p.age, p.gender, p.address as patient_address, pu.user_name as patient_name, pu.email as patient_mail FROM doctor as d, user as u, organization as o, patient as p, user as pu, doctor_patient as dp WHERE dp.doctor_id=d.doctor_id AND dp.patient_id=p.patient_id AND d.user_id = u.user_id AND p.user_id=pu.user_id AND d.organization_id = o.id AND u.user_name LIKE ?";
        return authPool
                .preparedQuery(sql)
                .execute(Tuple.of(doctorName))
                .onItem().transform(rows -> {
                    List<JsonObject> list = new ArrayList<>();

                    for(Row row : rows){
                        JsonObject json = new JsonObject()
                                .put("doctor_name", row.getString("doctor_name"))
                                .put("doctor_mail", row.getString("doctor_mail"))
                                .put("organization_name", row.getString("organization_name"))
                                .put("organization_address", row.getString("organization_address"))
                                .put("patient_name", row.getString("patient_name"))
                                .put("patient_mail", row.getString("patient_mail"))
                                .put("patient_age", row.getInteger("age"))
                                .put("patient_gender", row.getString("gender"))
                                .put("patient_address", row.getString("patient_address"));
                        list.add(json);
                    }

                    return list;
                });
    }

    @GET
    @Path("/getDoctorEncountersByDate/{employeeName}/{date}")
    public Uni<List<JsonObject>> getDoctorEncountersByDate(
            @PathParam("employeeName") String employeeName,
            @PathParam("date") String dateStr
    ) {
        LocalDateTime date;
        try {
            date = LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            return Uni.createFrom().failure(
                    new IllegalArgumentException("Invalid date format, expected e.g. 2025-11-27T10:45:50")
            );
        }

        String sql1 =
                "SELECT e.id, e.date_time, e.employee_id, e.patient_id, o.note " +
                        "FROM encounter e LEFT JOIN observation o ON e.id=o.encounter_id" +
                        " WHERE e.date_time = ?";

        return logsPool
                .preparedQuery(sql1)
                .execute(Tuple.of(date))
                .onItem().transformToUni(rows -> {
                    List<Row> encounterRows = rows.stream().toList();

                    if (encounterRows.isEmpty()) {
                        return Uni.createFrom().item(List.<JsonObject>of());
                    }

                    List<Long> employeeIds = new ArrayList<>();
                    List<Long> patientIds = new ArrayList<>();

                    for (Row row : encounterRows) {
                        Long eid = row.getLong("employee_id");
                        Long pid = row.getLong("patient_id");

                        if (eid != null && !employeeIds.contains(eid)) {
                            employeeIds.add(eid);
                        }
                        if (pid != null && !patientIds.contains(pid)) {
                            patientIds.add(pid);
                        }
                    }

                    if (employeeIds.isEmpty() || patientIds.isEmpty()) {
                        return Uni.createFrom().item(List.<JsonObject>of());
                    }

                    String empPlaceholder = String.join(
                            ",",
                            employeeIds.stream().map(x -> "?").toList()
                    );
                    String patPlaceholder = String.join(
                            ",",
                            patientIds.stream().map(x -> "?").toList()
                    );

                    String sql2 =
                            "SELECT e.employee_id, p.patient_id, " +
                                    "       eu.user_name AS employee_name, " +
                                    "       pu.user_name AS patient_name " +
                                    "FROM employee e " +
                                    "JOIN user eu ON e.user_id = eu.user_id " +
                                    "JOIN patient p ON 1=1 " +
                                    "JOIN user pu ON p.user_id = pu.user_id " +
                                    "WHERE eu.user_name LIKE ? " +
                                    "  AND e.employee_id IN (" + empPlaceholder + ") " +
                                    "  AND p.patient_id IN (" + patPlaceholder + ")";

                    List<Object> params = new ArrayList<>();
                    params.add(employeeName);
                    params.addAll(employeeIds);
                    params.addAll(patientIds);

                    return authPool
                            .preparedQuery(sql2)
                            .execute(Tuple.from(params))
                            .onItem().transform(rows2 -> {
                                // Maps: id -> name
                                Map<Long, String> employeeNames = new HashMap<>();
                                Map<Long, String> patientNames = new HashMap<>();

                                for (Row r : rows2) {
                                    Long eid = r.getLong("employee_id");
                                    Long pid = r.getLong("patient_id");

                                    if (eid != null) {
                                        employeeNames.put(eid, r.getString("employee_name"));
                                    }
                                    if (pid != null) {
                                        patientNames.put(pid, r.getString("patient_name"));
                                    }
                                }

                                List<JsonObject> result = new ArrayList<>();
                                for (Row er : encounterRows) {

                                    Long eid = er.getLong("employee_id");
                                    Long pid = er.getLong("patient_id");

                                    String empName = (eid != null) ? employeeNames.get(eid) : null;
                                    String patName = (pid != null) ? patientNames.get(pid) : null;

                                    if (empName == null) continue;

                                    JsonObject json = new JsonObject()
                                            .put("encounter_id", er.getLong("id"))
                                            .put("date_time", er.getLocalDateTime("date_time").toString())
                                            .put("employee_name", empName)
                                            .put("patient_name", patName)
                                            .put("note", er.getString("note"));

                                    result.add(json);
                                }

                                return result;
                            });
                });
    }

}
