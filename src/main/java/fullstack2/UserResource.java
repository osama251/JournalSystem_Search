package fullstack2;

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

@Path("/user")
public class UserResource {

    @Inject
    @Named("auth")
    MySQLPool authPool;

    @GET
    //@Produces(MediaType.APPLICATION_JSON)
    public Uni<List<JsonObject>> getAllUsers() {
        return authPool
            .query("SELECT user_id, user_name, email, role FROM user")
            .execute()
            .onItem().transform(rows -> {
                List<JsonObject> list = new ArrayList<>();
                for (Row row : rows) {
                    JsonObject json = new JsonObject()
                        .put("user_id", row.getLong("user_id"))
                        .put("user_name", row.getString("user_name"))
                        .put("email", row.getString("email"))
                        .put("role", row.getString("role"));
                    list.add(json);
                }
                return list;
            });
    }
}