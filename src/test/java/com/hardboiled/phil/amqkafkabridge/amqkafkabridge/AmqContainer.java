package com.hardboiled.phil.amqkafkabridge.amqkafkabridge;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class AmqContainer extends GenericContainer<AmqContainer> {
    public static final DockerImageName DEFAULT_IMAGE_NAME =
            DockerImageName.parse("mbrush/artemis:latest");

    public static final int AMQ_ADMIN_PORT = 8161;
    public static final int AMQ_AMQP_PORT = 61616;
    public static final String AMQ_LOGIN_CRED = "artemis";
    public static final String AMQ_LOGIN_PASSWORD = "artemis";

    public AmqContainer() {
        super(DEFAULT_IMAGE_NAME);
        this.waitStrategy = new HostPortWaitStrategy()
                .withStartupTimeout(Duration.of(10L, ChronoUnit.SECONDS));
        this.addExposedPorts(AMQ_AMQP_PORT, AMQ_ADMIN_PORT);
    }

}