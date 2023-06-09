package com.hardboiled.phil.amqkafkabridge.amqkafkabridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardboiled.phil.amqkafkabridge.records.TransferRecord;
import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpReceiver;
import io.vertx.core.buffer.Buffer;
import lombok.SneakyThrows;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.AMQ_TO_KAFKA_BYTES_IN;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.AMQ_TO_KAFKA_BYTES_OUT;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.AMQ_TO_KAFKA_JSON_IN;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.AMQ_TO_KAFKA_JSON_OUT;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.KAFKA_TO_AMQ_BYTES_IN;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.KAFKA_TO_AMQ_BYTES_OUT;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.KAFKA_TO_AMQ_JSON_IN;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.KAFKA_TO_AMQ_JSON_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KafkaResource.class)
@QuarkusTestResource(AmqResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AmqKafkaBridgeITest implements QuarkusTestAwaitility {

    AmqpClient amqpClient;
    ObjectMapper objectMapper;

    KafkaConsumer<String, String> kafkaStringConsumer;
    KafkaProducer<String, String> kafkaStringProducer;
    KafkaConsumer<String, byte[]> kafkaByteArrayConsumer;
    KafkaProducer<String, byte[]> kafkaByteArrayProducer;

    @BeforeAll
    void setUp() {
        Log.info("setUp called");
        AmqpClientOptions amqpClientOptions = new AmqpClientOptions()
                .setHost(AmqResource.getHost())
                .setPort(Integer.parseInt(AmqResource.getAmqpPort()))
                .setUsername(AmqResource.getUserName())
                .setPassword(AmqResource.getPassword());
        amqpClient = AmqpClient.create(amqpClientOptions);
        objectMapper = new ObjectMapper();
        kafkaStringConsumer = new KafkaConsumer<>(consumerProps(), new StringDeserializer(), new StringDeserializer());
        kafkaStringProducer = new KafkaProducer<>(producerProps(), new StringSerializer(), new StringSerializer());
        kafkaByteArrayConsumer = new KafkaConsumer<>(consumerProps(), new StringDeserializer(), new ByteArrayDeserializer());
        kafkaByteArrayProducer = new KafkaProducer<>(producerProps(), new StringSerializer(), new ByteArraySerializer());
    }

    @AfterAll
    void tearDown() {
        amqpClient.close();
        kafkaStringConsumer.close();
        kafkaStringProducer.close();
        kafkaByteArrayConsumer.close();
        kafkaByteArrayProducer.close();
    }

    @Test
    void testJsonMessageAmqToKafka() {
        var testJsonMessage = ResourceReader.readResourceToString("jsonTestMessage.json");
        kafkaStringConsumer.subscribe(Collections.singletonList(AMQ_TO_KAFKA_JSON_OUT));
        amqpClient.connect()
                .onComplete(asyncResult -> {
                    asyncResult.result()
                            .createSender(AMQ_TO_KAFKA_JSON_IN)
                            .onComplete(maybeSender ->
                                    maybeSender.result().send(AmqpMessage.create().withBody(testJsonMessage).build()));
                });
        await("assert testJsonMessageAmqToKafka").untilAsserted(() -> {
            final ConsumerRecords<String, String> messages = kafkaStringConsumer.poll(Duration.ofMillis(250));
            if (messages.iterator().hasNext()) {
                assertEquals(1, messages.count());
                assertEquals(testJsonMessage, messages.iterator().next().value());
            }
        });
    }

    @Test
    @SneakyThrows
    void testJsonMessageKafkaToAmq() {
        var testJsonMessage = ResourceReader.readResourceToString("jsonTestMessage.json");
        // metadata available for the kafka record if you need to get info out of it
        var metadata = kafkaStringProducer.send(
                new ProducerRecord<>(KAFKA_TO_AMQ_JSON_IN, "test-json::123", testJsonMessage)).get();

        final List<AmqpMessage> messages = new ArrayList<AmqpMessage>();
        amqpClient.createReceiver(KAFKA_TO_AMQ_JSON_OUT)
                .onComplete(maybeReceiver -> {
                    AmqpReceiver receiver = maybeReceiver.result();
                    receiver.handler(msg -> {
                        messages.add(msg);
                    });
                });
        await("assert testJsonMessageKafkaToAmq").untilAsserted(() -> {
            assertEquals(1, messages.size());
            assertEquals(testJsonMessage, messages.get(0).bodyAsString());
        });
    }

    @Test
    @SneakyThrows
    void testBytesMessageAmqToKafka() {
        var transferRecord = new TransferRecord("Stuff Goes Here");
        var contentJsonString = objectMapper.writeValueAsString(transferRecord);
        var buffer = Buffer.buffer(contentJsonString);
        var batchCompleteMessage = AmqpMessage.create().withBufferAsBody(buffer).build();

        kafkaByteArrayConsumer.subscribe(Collections.singletonList(AMQ_TO_KAFKA_BYTES_OUT));
        amqpClient
                .connect()
                .onComplete(asyncResult -> {
                    asyncResult.result()
                            .createSender(AMQ_TO_KAFKA_BYTES_IN)
                            .onComplete(maybeSender ->
                                    maybeSender.result().send(batchCompleteMessage));
                });
        await("assert testBytesMessageAmqToKafka").untilAsserted(() -> {
            final ConsumerRecords<String, byte[]> messages = kafkaByteArrayConsumer.poll(Duration.ofMillis(250));
            if (messages.iterator().hasNext()) {
                var outputBytes = messages.iterator().next().value();
                var marshalledRecord = objectMapper.readValue(outputBytes, TransferRecord.class);
                assertEquals(1, messages.count());
                assertEquals(transferRecord, marshalledRecord);
            }
        });
    }

    @Test
    @SneakyThrows
    void testBytesMessageKafkaToAmq() {
        var transferRecord = new TransferRecord("More Stuff Goes Here");
        var contentJsonString = objectMapper.writeValueAsString(transferRecord);
        var buffer = Buffer.buffer(contentJsonString);
        var bytesMessage = AmqpMessage.create().withBufferAsBody(buffer).build();

        kafkaByteArrayConsumer.subscribe(Collections.singletonList(KAFKA_TO_AMQ_BYTES_OUT));
        amqpClient.connect()
                .onComplete(asyncResult -> {
                    asyncResult.result()
                            .createSender(KAFKA_TO_AMQ_BYTES_IN)
                            .onComplete(maybeSender ->
                                    maybeSender.result().send(bytesMessage));
                });
        await("assert testBytesMessageKafkaToAmq").untilAsserted(() -> {
            final ConsumerRecords<String, byte[]> messages = kafkaByteArrayConsumer.poll(Duration.ofMillis(250));
            if (messages.iterator().hasNext()) {
                var outputBytes = messages.iterator().next().value();
                var marshalledRecord = objectMapper.readValue(outputBytes, TransferRecord.class);
                assertEquals(1, messages.count());
                assertEquals(transferRecord, marshalledRecord);
            }
        });
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        Log.info("Consumer Props is getting bootstrap of " + KafkaResource.getBootstrapServers());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-id");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        Log.info("Producer Props is getting bootstrap of " + KafkaResource.getBootstrapServers());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaResource.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2_100);
        return props;
    }

}
