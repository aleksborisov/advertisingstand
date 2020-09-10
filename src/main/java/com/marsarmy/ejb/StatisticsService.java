package com.marsarmy.ejb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marsarmy.model.ProductStatistics;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.log4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.faces.annotation.FacesConfig;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

/**
 * Service responsible for operations on statistics
 */
@Singleton
@ApplicationScoped
@FacesConfig
public class StatisticsService {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    @Push(channel = "push")
    private PushContext push;

    private List<ProductStatistics> statistics;

    private static final Logger LOGGER = Logger.getLogger(StatisticsService.class);

    /**
     * Returns the list of product statistics for top 10 products from class field.
     * Gives access to the list from JSF.
     *
     * @return {@link List} of {@link ProductStatistics}
     */
    @Named
    @Produces
    public List<ProductStatistics> getStatistics() {
        return statistics;
    }

    /**
     * Returns the list of product statistics for top 10 products from REST WebService.
     *
     * @return {@link List} of {@link ProductStatistics}
     */
    public List<ProductStatistics> getDataFromWebService() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/stand");
        Response response = target.request().get();
        String jsonArray = response.readEntity(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<ProductStatistics> result = new ArrayList<>();
        try {
            //noinspection unchecked
            result = mapper.readValue(jsonArray, List.class);
        } catch (JsonProcessingException e) {
            LOGGER.error(e.getMessage(), e);
        }

        LOGGER.info("Received statistics from the server");

        return result;
    }

    /**
     * Initiates field 'statistics'.
     * Creates {@link MessageListener} to update statistics.
     */
    @PostConstruct
    public void init() {
        this.statistics = getDataFromWebService();
        LOGGER.info("Statistics saved on the client side");

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("failover://(tcp://localhost:61616)");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("UpdateTopic");
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(message -> {
                LOGGER.info("Received a message from the server about the availability of statistics update");
                this.statistics = getDataFromWebService();
                LOGGER.info("Statistics saved on the client side");
                push.send("push");
                LOGGER.info("Message pushed to the websocket");
            });
        } catch (JMSException e) {
            LOGGER.error("Error: " + e.getMessage(), e);
        }
    }
}
