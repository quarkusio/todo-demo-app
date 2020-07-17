package io.quarkus.sample;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.eclipse.microprofile.openapi.annotations.Operation;

@Path("/")
public class IndexResource {

    @GET
    @Operation(hidden = true)
    public Response redirect() {
        URI redirect = UriBuilder.fromUri("todo.html").build();
        return Response.temporaryRedirect(redirect).build();
    }
}