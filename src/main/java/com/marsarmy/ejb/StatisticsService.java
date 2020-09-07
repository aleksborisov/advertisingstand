package com.marsarmy.ejb;

import com.marsarmy.model.ProductStatistics;
import org.apache.activemq.ActiveMQConnectionFactory;

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
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
@ApplicationScoped
@FacesConfig
public class StatisticsService {

    @Inject
    @Push(channel = "push")
    private PushContext push;

    private List<ProductStatistics> statistics;

    @Named
    @Produces
    public List<ProductStatistics> getStatistics() {
        return statistics;
    }

    public List<ProductStatistics> getDataFromWebService() {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target("http://localhost:8081/stand");
        Response response = target.request().get();
        JsonArray jsonArray = response.readEntity(JsonArray.class);

        return jsonArray.stream().map(json -> {
            ProductStatistics productStatistics = new ProductStatistics();
            productStatistics.setUpc(((JsonObject) json).getJsonNumber("upc").longValue());
            productStatistics.setName(((JsonObject) json).getString("name"));
            productStatistics.setColor(((JsonObject) json).getString("color"));
            productStatistics.setBrand(((JsonObject) json).getString("brand"));
            productStatistics.setCategory(((JsonObject) json).getString("category"));
            productStatistics.setPrice(((JsonObject) json).getInt("price"));
            productStatistics.setQuantitySold(((JsonObject) json).getJsonNumber("quantitySold").longValue());
            System.out.println(productStatistics.toString());
            return productStatistics;
        }).collect(Collectors.toList());
    }

    @PostConstruct
    public void init() {
        this.statistics = getDataFromWebService();
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("failover://(tcp://localhost:61616)");
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Topic topic = session.createTopic("UpdateTopic");
            MessageConsumer consumer = session.createConsumer(topic);
            consumer.setMessageListener(message -> {
                this.statistics = getDataFromWebService();
                push.send("push");
            });
        } catch (JMSException e) {
            e.printStackTrace();
        }
    }
}
