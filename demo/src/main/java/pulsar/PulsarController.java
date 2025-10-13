package pulsar;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import model.Order;

import org.apache.pulsar.client.api.PulsarClient;
import org.carl.infrastructure.component.web.annotations.ControllerLogged;
import org.carl.infrastructure.dto.SingleEntityResponse;
import org.carl.infrastructure.pulsar.builder.IConsumer;
import org.carl.infrastructure.pulsar.builder.IProducer;
import org.carl.infrastructure.pulsar.builder.PulsarConsumerBuilder;
import org.carl.infrastructure.pulsar.builder.PulsarProducerBuilder;
import org.carl.infrastructure.pulsar.common.ex.ConsumerException;
import org.carl.infrastructure.pulsar.common.ex.ProducerException;

@Path("/pulsar")
@ControllerLogged
public class PulsarController {

    @Inject PulsarClient client;
    IProducer<Order> producer;
    IConsumer<Order> consumer;

    @PostConstruct
    public void init() throws ProducerException, ConsumerException {
        String TOPIC = "order";
        producer = PulsarProducerBuilder.create(client, Order.class).topic(TOPIC).create();
        consumer =
                PulsarConsumerBuilder.create(client, Order.class)
                        .subscriptionName("sub1")
                        .messageListener(
                                (consumer, msg) -> {
                                    System.out.println("---------" + msg.getValue() + "----------");
                                    try {
                                        consumer.acknowledge(msg);
                                    } catch (ConsumerException e) {
                                        consumer.negativeAcknowledge(msg);
                                        throw new RuntimeException(e);
                                    }
                                })
                        .topic("user")
                        .subscribe();
    }

    @POST()
    @Path("/send")
    @Produces
    public SingleEntityResponse<IProducer.SendResult<Order>> send(Order order)
            throws ProducerException {
        IProducer.SendResult<Order> orderSendResult = producer.sendMessage(order);
        return SingleEntityResponse.of(orderSendResult);
    }
}
