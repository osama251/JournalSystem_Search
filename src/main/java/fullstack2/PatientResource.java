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
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Path("/patient")
public class PatientResource {

    @Inject
    @Named("logs")
    MySQLPool logsPool;

    @Inject
    @Named("auth")
    MySQLPool authPool;

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm")
    String realm;


    @GET
    @Path("/getByName/{patientName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getPatientByName(@PathParam("patientName") String patientName) {

        return Uni.createFrom().item(() -> {

            List<UserRepresentation> users =
                    keycloak.realm(realm)
                            .users()
                            .searchByUsername(patientName, false);

            List<JsonObject> patients = new ArrayList<>();

            for (UserRepresentation user : users) {

                Map<String, List<String>> attrs = user.getAttributes();

                JsonObject obj = new JsonObject()
                        .put("user_id", user.getId())
                        .put("user_name", user.getUsername())
                        .put("email", user.getEmail())
                        .put("age", getAttr(attrs, "age"))
                        .put("gender", getAttr(attrs, "gender"))
                        .put("address", getAttr(attrs, "address"));

                patients.add(obj);
            }

            return patients;
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
    @Path("/getByCondition/{conditionName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getPatientsByCondition(@PathParam("conditionName") String conditionName) {

        String sql = "SELECT patient_id FROM sickness WHERE condition_name LIKE ?";

        return logsPool
                .preparedQuery(sql)
                .execute(Tuple.of(conditionName))
                .onItem().transformToUni(rows -> {

                    // 1) Collect patient IDs from logs DB
                    List<String> patientIds = new ArrayList<>();
                    for (Row row : rows) {
                        // patient_id is now your Keycloak user id (recommended: VARCHAR(36))
                        String pid = row.getString("patient_id");
                        if (pid != null && !pid.isBlank()) {
                            patientIds.add(pid);
                        }
                    }

                    if (patientIds.isEmpty()) {
                        return Uni.createFrom().item(List.<JsonObject>of());
                    }

                    // 2) Fetch user info from Keycloak for those IDs
                    return Uni.createFrom().item(() -> {
                        List<JsonObject> result = new ArrayList<>();

                        for (String userId : patientIds) {
                            UserRepresentation user = keycloak.realm(realm)
                                    .users()
                                    .get(userId)
                                    .toRepresentation();

                            Map<String, List<String>> attrs =
                                    (Map<String, List<String>>) user.getAttributes();

                            JsonObject json = new JsonObject()
                                    .put("user_id", user.getId())
                                    .put("user_name", user.getUsername())
                                    .put("email", user.getEmail())
                                    .put("address", getAttr(attrs, "address"))
                                    .put("age", getAttr(attrs, "age"))
                                    .put("gender", getAttr(attrs, "gender"));

                            result.add(json);
                        }

                        return result;
                    });
                });
    }

    @GET
    @Path("/getByGender/{gender}")
    public Uni<List<JsonObject>> getPatientsByGender(@PathParam("gender") String gender) {

        return Uni.createFrom().item(() -> {

            List<JsonObject> result = new ArrayList<>();

            // Search users by attribute gender
            List<UserRepresentation> users =
                    keycloak.realm(realm)
                            .users()
                            .searchByAttributes("gender:" + gender);

            for (UserRepresentation user : users) {

                Map<String, List<String>> attrs = user.getAttributes();

                JsonObject json = new JsonObject()
                        .put("user_id", user.getId())               // Keycloak UUID
                        .put("user_name", user.getUsername())
                        .put("email", user.getEmail())
                        .put("gender", getAttr(attrs, "gender"))
                        .put("age", getAttr(attrs, "age"))
                        .put("address", getAttr(attrs, "address"));

                result.add(json);
            }

            return result;
        });
    }

    @GET
    @Path("/getByAge/{age}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getPatientsByAge(@PathParam("age") int age) {

        return Uni.createFrom().item(() -> {

            List<JsonObject> result = new ArrayList<>();

            // Keycloak stores attributes as strings, so compare using String
            String ageStr = String.valueOf(age);

            // Option A (recommended): fetch by attribute search if supported in your KC version
            List<UserRepresentation> users =
                    keycloak.realm(realm)
                            .users()
                            .searchByAttributes("age:" + ageStr);

            for (UserRepresentation user : users) {
                Map<String, List<String>> attrs = user.getAttributes();

                JsonObject json = new JsonObject()
                        .put("user_id", user.getId()) // Keycloak UUID
                        .put("user_name", user.getUsername())
                        .put("email", user.getEmail())
                        .put("address", getAttr(attrs, "address"))
                        .put("age", getAttr(attrs, "age"))
                        .put("gender", getAttr(attrs, "gender"));

                result.add(json);
            }

            return result;
        });
    }

}
