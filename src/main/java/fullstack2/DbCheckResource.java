package fullstack2;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.mysqlclient.MySQLPool;
import io.vertx.mutiny.sqlclient.Row;
import io.vertx.mutiny.sqlclient.RowSet;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.quarkus.reactive.datasource.ReactiveDataSource; // for @ReactiveDataSource

@Path("/dbcheck")
public class DbCheckResource {
/*
    @Inject
    @io.quarkus.reactive.datasource.ReactiveDataSource("auth")
    MySQLPool authPool;

    @Inject
    @io.quarkus.reactive.datasource.ReactiveDataSource("logs")
    MySQLPool logsPool;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> check() {
        Uni<RowSet<Row>> auth = authPool.query("SELECT 1").execute();
        Uni<RowSet<Row>> logs = logsPool.query("SELECT 1").execute();

        return Uni.combine().all().unis(auth, logs).asTuple()
                .map(t -> "OK: auth-db & logs-db reachable");
    }*/

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String dbcheck() {
        return "dbcheck";
    }
}