package fullstack2;

import io.quarkus.security.Authenticated;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Path("/user")
@Produces(MediaType.APPLICATION_JSON)
@Authenticated
public class UserResource {

    @Inject
    Keycloak keycloak;

    @ConfigProperty(name = "keycloak.realm")
    String realm;

    @GET
    public Uni<List<JsonObject>> getAllUsers() {
        return Uni.createFrom().item(() -> {
            // fetch users from Keycloak
            List<UserRepresentation> users =
                    keycloak.realm(realm).users().list();

            List<JsonObject> list = new ArrayList<>();

            for (UserRepresentation u : users) {
                // optional: fetch realm roles (can be slow if many users)
                List<RoleRepresentation> roles = keycloak.realm(realm)
                        .users()
                        .get(u.getId())
                        .roles()
                        .realmLevel()
                        .listAll();

                String role = roles.stream()
                        .map(RoleRepresentation::getName)
                        .filter(r -> !r.startsWith("default-roles-"))
                        .findFirst()
                        .orElse(null);

                JsonObject json = new JsonObject()
                        .put("user_id", u.getId())          // Keycloak ID is String (UUID)
                        .put("user_name", u.getUsername())
                        .put("email", u.getEmail())
                        .put("role", role);

                list.add(json);
            }

            return list;
        });
    }
}