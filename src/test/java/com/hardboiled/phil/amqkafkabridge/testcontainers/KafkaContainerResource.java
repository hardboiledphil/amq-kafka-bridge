package com.hardboiled.phil.amqkafkabridge.testcontainers;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import java.util.concurrent.TimeUnit;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class KafkaContainerResource implements QuarkusTestResourceLifecycleManager {

    private static KafkaContainer kafkaContainer;

    @Override
    public Map<String, String> start() {
        if (kafkaContainer == null) {
            kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.3.1"))
                    .withEmbeddedZookeeper()
                    .withNetwork(TestNetworkResource.getNetwork())
                    .withNetworkAliases("kafka")
                    .withExposedPorts(9092, 9093)
                    .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true") // don't do this in production
                    .waitingFor(Wait.forListeningPort());
        }
        log.info("CONTAINER -> Starting Kafka container");
        kafkaContainer.start();
        await().pollDelay(500, TimeUnit.MILLISECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .timeout(10000, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    log.info("kafkaContainer.isRunning() => {}", kafkaContainer.isRunning());
                    assertTrue(kafkaContainer.isRunning());
                });
        log.info("getNetworkedBootstrapServers() =>  {}", getNetworkedBootstrapServers());
        log.info("kafkaContainer.getMappedPort(9092) =>  {}", kafkaContainer.getMappedPort(9092));
        log.info("kafkaContainer.getMappedPort(9093) =>  {}", kafkaContainer.getMappedPort(9093));
        log.info("kafkaContainer.getEnvMap() =>  {}", kafkaContainer.getEnvMap());
        log.info("kafkaContainer.getHost() =>  {}", kafkaContainer.getHost());

        return Collections.singletonMap("kafka.bootstrap.servers", getNetworkedBootstrapServers());
    }

    @Override
    public void stop() {
        log.info("CONTAINER -> Stopping Kafka Container");
        Optional.ofNullable(kafkaContainer).ifPresent(GenericContainer::stop);
    }

    public static String getBootstrapServers() {
        return kafkaContainer.getBootstrapServers();
    }

    public static String getNetworkedBootstrapServers() {
        return "localhost:" + kafkaContainer.getMappedPort(9093);
    }

}
