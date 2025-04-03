package org.carl.infrastructure.protocol;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

@Path("/rest")
public interface BaseDataService {
    @POST
    @Path("jy002_base34")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    Response getEnterpriseByCreditCode(@RestForm MultivaluedMap<String, String> body);
}
