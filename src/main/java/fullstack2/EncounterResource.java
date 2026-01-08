package fullstack2;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;

@Path("/encounter")
@Authenticated
public class EncounterResource {

    @Inject
    @Named("logs")
    MySQLPool logsPool;

    @GET
    //@Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getAllEncounters() {
        return logsPool
                .query("SELECT id, date_time, doctor_id, patient_id FROM encounter")
                .execute()
                .onItem().transform(rows -> {
                    List<JsonObject> list = new ArrayList<>();
                    for (Row row : rows) {
                        JsonObject json = new JsonObject()
                                .put("id", row.getLong("id"))
                                .put("date_time", row.getLocalDateTime("date_time").toString())
                                .put("doctor_id", row.getString("doctor_id"))
                                .put("patient_id", row.getString("patient_id"));
                        list.add(json);
                    }
                    return list;
                });
    }
}