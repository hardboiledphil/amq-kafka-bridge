package com.hardboiled.phil.amqkafkabridge.amqkafkabridge;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

public class AmqResource implements QuarkusTestResourceLifecycleManager {

    private final static AmqContainer amq = new AmqContainer();

    @Override
    public Map<String, String> start() {
        amq.start();
        return Map.of(
                "amqp.host", getHost(),
                "amqp.port", getAmqpPort(),
                "amqp.username", getUserName(),
                "amqp.password", getPassword()
        );
    }

    @Override
    public void stop() {
        amq.close();
    }

    public static String getHost() { return amq.getHost(); }

    public static String getAmqpPort() { return Integer.toString(amq.getMappedPort(AmqContainer.AMQ_AMQP_PORT)); }

    public static String getUserName() { return AmqContainer.AMQ_LOGIN_CRED; }

    public static String getPassword() { return AmqContainer.AMQ_LOGIN_PASSWORD; }
}
