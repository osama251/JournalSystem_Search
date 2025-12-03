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

import java.util.ArrayList;
import java.util.List;

@Path("/patient")
public class PatientResource {

    @Inject
    @Named("logs")
    MySQLPool logsPool;

    @Inject
    @Named("auth")
    MySQLPool authPool;


    @GET
    @Path("/getByName/{patientName}")
    public Uni<JsonObject> getPatientByName(@PathParam("patientName") String patientName){
        String sql = "SELECT user_name, email, patient_id, address, age, gender, p.user_id FROM patient as p, user as u WHERE user_name LIKE ? AND p.user_id = u.user_id";
        return authPool
                .preparedQuery(sql)
                .execute(Tuple.of(patientName))
                .onItem().transform(rows -> {
                    Row row = rows.iterator().next();
                    return new JsonObject()
                            .put("user_name", row.getString("user_name"))
                            .put("email", row.getString("email"))
                            .put("patient_id", row.getLong("patient_id"))
                            .put("address", row.getString("address"))
                            .put("age", row.getInteger("age"))
                            .put("gender", row.getString("gender"))
                            .put("user_id", row.getLong("user_id"));
                });
    }


    @GET
    @Path("/getByCondition/{conditionName}")
    public Uni<List<JsonObject>> getPatientsByCondition(@PathParam("conditionName") String conditionName){
        String sql = "SELECT patient_id FROM sickness WHERE condition_name LIKE ?";
        return logsPool
            .preparedQuery(sql)
            .execute(Tuple.of(conditionName))
            .onItem().transformToUni(rows -> {

                List<Long> patientIds = new ArrayList<>();
                for(Row row : rows){
                    Long pid = row.getLong("patient_id");
                    if(pid != null){
                        patientIds.add(pid);
                    }
                }
                if(patientIds.isEmpty()){
                    return Uni.createFrom().item(List.<JsonObject>of());
                }

                String placeHolders = String.join(
                        ",",
                        patientIds.stream().map(x -> "?").toList()
                );

                String sqlPatients =
                        "SELECT u.user_name, u.email, p.patient_id, p.address, p.age, p.gender, p.user_id FROM patient p JOIN user u ON p.user_id = u.user_id WHERE p.patient_id IN (" + placeHolders + ")";

                return authPool
                        .preparedQuery(sqlPatients)
                        .execute(Tuple.from(patientIds))
                        .onItem().transform(patientRows -> {
                            List<JsonObject> list = new ArrayList<>();
                            for (Row row : patientRows) {
                                JsonObject json = new JsonObject()
                                        .put("user_name", row.getString("user_name"))
                                        .put("email", row.getString("email"))
                                        .put("patient_id", row.getLong("patient_id"))
                                        .put("address", row.getString("address"))
                                        .put("age", row.getInteger("age"))
                                        .put("gender", row.getString("gender"))
                                        .put("user_id", row.getLong("user_id"));
                                list.add(json);
                            }
                            return list;
                        });
            });

        //return null;
    }

    @GET
    @Path("/getByGender/{gender}")
    public Uni<List<JsonObject>> getPatientsByGender(@PathParam("gender") String gender){
        String sql = "SELECT user_name, email, patient_id, address, age, gender, p.user_id FROM patient as p, user as u WHERE gender LIKE ? AND p.user_id = u.user_id";
        return authPool
                .preparedQuery(sql)
                .execute(Tuple.of(gender))
                .onItem().transform(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (Row row : rows) {
                        JsonObject json = new JsonObject()
                                .put("user_name", row.getString("user_name"))
                                .put("email", row.getString("email"))
                                .put("patient_id", row.getLong("patient_id"))
                                .put("address", row.getString("address"))
                                .put("age", row.getInteger("age"))
                                .put("gender", row.getString("gender"))
                                .put("user_id", row.getLong("user_id"));
                        list.add(json);
                    }
                    return list;
                });
    }

    @GET
    @Path("/getByAge/{age}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getPatientsByAge(@PathParam("age") int age){
        String sql = "SELECT user_name, email, patient_id, address, age, gender, p.user_id FROM patient as p, user as u WHERE age = ? AND p.user_id = u.user_id";
        return authPool
                .preparedQuery(sql)
                .execute(Tuple.of(age))
                .onItem().transform(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (Row row : rows) {
                        JsonObject json = new JsonObject()
                                .put("user_name", row.getString("user_name"))
                                .put("email", row.getString("email"))
                                .put("patient_id", row.getLong("patient_id"))
                                .put("address", row.getString("address"))
                                .put("age", row.getInteger("age"))
                                .put("gender", row.getString("gender"))
                                .put("user_id", row.getLong("user_id"));
                        list.add(json);
                    }
                    return list;
                });
    }

}
