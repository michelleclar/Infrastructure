package pulsar;

import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.jboss.logging.Logger;

import pulsar.producer.PulsarPriceProducer;

@Path("/pulsar")
@ControllerLogged
public class PulsarController {

    @Inject Logger logger;
    @Inject PulsarPriceProducer pulsarPriceProducer;

    @POST
    @Produces
    public String executer() {
        System.out.println(pulsarPriceProducer.generate());

        return "success";
    }
}
