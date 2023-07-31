package com.hardboiled.phil.amqkafkabridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardboiled.phil.amqkafkabridge.records.TransferRecord;
import com.hardboiled.phil.amqkafkabridge.records.TransferRecordFail;
import com.hardboiled.phil.amqkafkabridge.testcontainers.AmqContainerResource;
import com.hardboiled.phil.amqkafkabridge.testcontainers.KafkaContainerResource;
import com.hardboiled.phil.amqkafkabridge.testcontainers.QuarkusTestAwaitility;
import io.quarkus.logging.Log;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.amqp.AmqpClient;
import io.vertx.amqp.AmqpClientOptions;
import io.vertx.amqp.AmqpMessage;
import io.vertx.amqp.AmqpReceiver;
import io.vertx.core.buffer.Buffer;
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
import java.util.concurrent.ExecutionException;

import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.AMQ_BYTES_IN;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.DLQ_QUEUE;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.DLQ_TOPIC;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.KAFKA_BYTES_IN;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.AMQ_BYTES_OUT;
import static com.hardboiled.phil.amqkafkabridge.AmqKafkaBridge.KAFKA_BYTES_OUT;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KafkaContainerResource.class)
@QuarkusTestResource(AmqContainerResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AmqKafkaBridgeITest implements QuarkusTestAwaitility {

    AmqpClient amqpClient;
    ObjectMapper objectMapper;

    KafkaConsumer<String, byte[]> kafkaByteArrayConsumer;
    KafkaProducer<String, byte[]> kafkaByteArrayProducer;

    @BeforeAll
    void setUp() {
        Log.info("setUp called");

        Log.info("host -> " + AmqContainerResource.getNetworkHost());
        Log.info("amqp port -> " + AmqContainerResource.getAmqpPort());
        AmqpClientOptions amqpClientOptions = new AmqpClientOptions()
                .setHost(AmqContainerResource.getNetworkHost())
                .setPort(Integer.parseInt(AmqContainerResource.getAmqpPort()))
                .setUsername(AmqContainerResource.getUserName())
                .setPassword(AmqContainerResource.getPassword());
        amqpClient = AmqpClient.create(amqpClientOptions);
        objectMapper = new ObjectMapper();
        kafkaByteArrayConsumer = new KafkaConsumer<>(consumerProps(), new StringDeserializer(), new ByteArrayDeserializer());
        kafkaByteArrayProducer = new KafkaProducer<>(producerProps(), new StringSerializer(), new ByteArraySerializer());
    }

    @AfterAll
    void tearDown() {
        amqpClient.close();
        kafkaByteArrayConsumer.close();
        kafkaByteArrayProducer.close();
    }

    @Test
    void testBytesMessageKafkaToAmq() throws JsonProcessingException, ExecutionException, InterruptedException {
        var transferRecord = new TransferRecord("More Stuff Goes Here");
        var contentBytes = objectMapper.writeValueAsBytes(transferRecord);

        var metadata = kafkaByteArrayProducer.send(
                new ProducerRecord<>(KAFKA_BYTES_IN, "test-json::123a", contentBytes)).get();

        final List<AmqpMessage> messages = new ArrayList<AmqpMessage>();
        amqpClient.createReceiver(AMQ_BYTES_OUT)
                .onComplete(maybeReceiver -> {
                    AmqpReceiver receiver = maybeReceiver.result();
                    receiver.handler(msg -> {
                        messages.add(msg);
                    });
                });
        await("assert testBytesMessageKafkaToAmq").untilAsserted(() -> {
            assertEquals(1, messages.size());
            var content = objectMapper.readValue(messages.get(0).bodyAsBinary().getBytes(), TransferRecord.class);
            assertEquals(new TransferRecord(transferRecord.stuffGoesHere().concat("-processed-kafka-to-amq")) , content);
            Log.info("Asserting completed for testBytesMessageKafkaToAmq");
        });
    }

    @Test
    void testBytesMessageKafkaToAmqWrongObject() throws JsonProcessingException, ExecutionException, InterruptedException {
        var transferRecord = new TransferRecordFail("More Stuff Goes Here");
        var contentBytes = objectMapper.writeValueAsBytes(transferRecord);

        var metadata = kafkaByteArrayProducer.send(
                new ProducerRecord<>(KAFKA_BYTES_IN, "test-json::123b", contentBytes)).get();

        kafkaByteArrayConsumer.subscribe(Collections.singletonList(DLQ_TOPIC));

        await("assert testBytesMessageKafkaToAmqWrongObject").untilAsserted(() -> {
            final ConsumerRecords<String, byte[]> messages = kafkaByteArrayConsumer.poll(Duration.ofMillis(250));
            if (messages.iterator().hasNext()) {
                assertEquals(1, messages.count());
                var record = messages.iterator().next();
                assertEquals("test-json::123b", record.key());
                Log.info("DLQ caught the message");
            }
        });
    }

    @Test
    void testBytesMessageAmqToKafkaWrongObject() throws JsonProcessingException {
        var transferRecord = new TransferRecordFail("More Stuff Goes Here");
        var contentBytes = objectMapper.writeValueAsBytes(transferRecord);
        var buffer = Buffer.buffer(contentBytes);
        var amqMessage = AmqpMessage.create().withBufferAsBody(buffer).build();

        amqpClient.connect()
                .onComplete(asyncResult -> {
                    asyncResult.result()
                            .createSender(AMQ_BYTES_IN)
                            .onComplete(maybeSender ->
                                    maybeSender.result().send(amqMessage));
                });

        final List<AmqpMessage> messages = new ArrayList<AmqpMessage>();
        amqpClient.createReceiver(DLQ_QUEUE)
                .onComplete(maybeReceiver -> {
                    AmqpReceiver receiver = maybeReceiver.result();
                    receiver.handler(msg -> {
                        messages.add(msg);
                    });
                });
        await("assert testBytesMessageAmqToKafka").untilAsserted(() -> {
            assertEquals(1, messages.size());
            var content = objectMapper.readValue(messages.get(0).bodyAsBinary().getBytes(), TransferRecord.class);
            assertEquals(new TransferRecord(transferRecord.stuffGoesHereX().concat("-processed-amq-to-kafka")) , content);
            Log.info("Asserting completed for testBytesMessageAmqToKafka");
        });

//        kafkaByteArrayConsumer.subscribe(Collections.singletonList(KAFKA_BYTES_OUT));
//
//        await("assert testBytesMessageAmqToKafka").untilAsserted(() -> {
//            final ConsumerRecords<String, byte[]> messages = kafkaByteArrayConsumer.poll(Duration.ofMillis(250));
//            if (messages.iterator().hasNext()) {
//                var outputBytes = messages.iterator().next().value();
//                var marshalledRecord = objectMapper.readValue(outputBytes, TransferRecord.class);
//                assertEquals(1, messages.count());
//                assertEquals(transferRecord, marshalledRecord.stuffGoesHere());
//            }
//        });
    }

    @Test
    void testBytesMessageAmqToKafka() throws JsonProcessingException {
        var transferRecord = new TransferRecord("More Stuff Goes Here");
        var contentBytes = objectMapper.writeValueAsBytes(transferRecord);
        var buffer = Buffer.buffer(contentBytes);
        var amqMessage = AmqpMessage.create().withBufferAsBody(buffer).build();

        amqpClient.connect()
                .onComplete(asyncResult -> {
                    asyncResult.result()
                            .createSender(AMQ_BYTES_IN)
                            .onComplete(maybeSender ->
                                    maybeSender.result().send(amqMessage));
                });

        kafkaByteArrayConsumer.subscribe(Collections.singletonList(KAFKA_BYTES_OUT));

        await("assert testBytesMessageAmqToKafka").untilAsserted(() -> {
            final ConsumerRecords<String, byte[]> messages = kafkaByteArrayConsumer.poll(Duration.ofMillis(250));
            if (messages.iterator().hasNext()) {
                var outputBytes = messages.iterator().next().value();
                var marshalledRecord = objectMapper.readValue(outputBytes, TransferRecord.class);
                assertEquals(1, messages.count());
                assertEquals(transferRecord, marshalledRecord.stuffGoesHere());
            }
        });
    }

    private Properties consumerProps() {
        Properties props = new Properties();
        Log.info("Consumer Props is getting bootstrap of " + KafkaContainerResource.getBootstrapServers());
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerResource.getBootstrapServers());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-id");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    private Properties producerProps() {
        Properties props = new Properties();
        Log.info("Producer Props is getting bootstrap of " + KafkaContainerResource.getBootstrapServers());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaContainerResource.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 2_100);
        return props;
    }

}
