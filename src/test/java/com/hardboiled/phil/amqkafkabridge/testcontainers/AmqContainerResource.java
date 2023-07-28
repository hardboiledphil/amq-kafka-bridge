package com.hardboiled.phil.amqkafkabridge.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class AmqContainerResource implements QuarkusTestResourceLifecycleManager {

    private static AmqContainer amqContainer;

    @Override
    public Map<String, String> start() {
        if (amqContainer == null) {
            amqContainer = new AmqContainer()
                    .withNetwork(TestNetworkResource.getNetwork())
                    .withNetworkAliases("amq");
        }

        amqContainer.start();

        await().pollDelay(1000, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .timeout(5000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertTrue(amqContainer.isRunning()));

        log.info("CONTAINER -> Starting AMQ Container host: {} port: {}", getNetworkHost(), getAmqpPort());

        return Map.of(
                "amqp.host", getNetworkHost(),
                "amqp.port", getAmqpPort(),
                "amqp.username", getUserName(),
                "amqp.password", getPassword()
        );
    }

    @Override
    public void stop() {
        log.info("CONTAINER -> Stopping AMQ Container");
        Optional.ofNullable(amqContainer).ifPresent(GenericContainer::stop);
    }

    public static String getHost() { return amqContainer.getHost(); }

    public static String getAmqpPort() { return Integer.toString(amqContainer.getMappedPort(AmqContainer.AMQ_AMQP_PORT)); }

    public static String getUserName() { return AmqContainer.AMQ_LOGIN_CRED; }

    public static String getPassword() { return AmqContainer.AMQ_LOGIN_PASSWORD; }

    public static String getNetworkHost() { return "localhost"; }


}
