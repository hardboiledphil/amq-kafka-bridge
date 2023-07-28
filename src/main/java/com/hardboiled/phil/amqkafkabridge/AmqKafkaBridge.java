package com.hardboiled.phil.amqkafkabridge;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.eclipse.microprofile.reactive.messaging.Message.LOGGER;

import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.util.Random;
import java.util.concurrent.CompletionStage;

/**
 * Main component used for bridging between Kafka and AMQ for GEM/EOD related message flows
 *
 * <p>Some of the flows are just XML messages and will be transferred as strings.
 * Some of the flows represent java objects and will aim to be proto based but are just byte[] for now
 */
public class AmqKafkaBridge {

  public static final String AMQ_TO_KAFKA_JSON_IN = "AMQ.TO.KAFKA.JSON.IN";
  public static final String AMQ_TO_KAFKA_JSON_OUT = "amq-to-kafka-json-out";
  public static final String KAFKA_TO_AMQ_JSON_IN = "kafka-to-amq-json-in";
  public static final String KAFKA_TO_AMQ_JSON_OUT = "KAFKA.TO.AMQ.JSON.OUT";

  public static final String AMQ_TO_KAFKA_BYTES_IN = "AMQ.TO.KAFKA.BYTES.IN";
  public static final String AMQ_TO_KAFKA_BYTES_OUT = "amq-to-kafka-bytes-out";
  public static final String KAFKA_TO_AMQ_BYTES_IN = "kafka-to-amq-bytes-in";
  public static final String KAFKA_TO_AMQ_BYTES_OUT = "KAFKA.TO.AMQ.BYTES.OUT";

  @Incoming("amq-to-kafka-json-in") // AMQ queue
  @Outgoing("amq-to-kafka-json-out") // Kafka topic
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS, retryOn = IllegalCallerException.class)
  @Acknowledgment(Acknowledgment.Strategy.MANUAL)
  public Message<String> jsonFromAmqToKafka(Message<String> amqJsonMessage) {
    Log.info("jsonFromAmqToKafka triggered");
    return amqJsonMessage;
  }

  @Incoming("kafka-to-amq-json-in") // Kafka Topic
  @Outgoing("kafka-to-amq-json-out") // AMQ queue
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS, retryOn = IllegalCallerException.class)
  @Acknowledgment(Acknowledgment.Strategy.MANUAL)
  public Message<String> jsonFromKafkaToAmq(Message<String> kafkaJsonMessage) {
    Log.info("jsonFromKafkaToAmq triggered");
    if (new Random().nextInt(5) > 0) {
      throw new IllegalArgumentException("Doh!");
    }
    return Message.of(kafkaJsonMessage.getPayload(), kafkaJsonMessage.getMetadata(), kafkaJsonMessage::ack);
  }

  @Incoming("amq-to-kafka-bytes-in") // AMQ queue
  @Outgoing("amq-to-kafka-bytes-out") // Kafka topic
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS, retryOn = IllegalCallerException.class)
  @Acknowledgment(Acknowledgment.Strategy.MANUAL)
  public Message<byte[]> bytesFromAmqToKafka(Message<byte[]> amqBytesMessage) {
    Log.info("bytesFromAmqToKafka triggered");
    return amqBytesMessage;
  }

  @Incoming("kafka-to-amq-bytes-in") // kafka topic
  @Outgoing("kafka-to-amq-bytes-out") // AMQ queue
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS, retryOn = IllegalCallerException.class)
  @Acknowledgment(Acknowledgment.Strategy.MANUAL)
  public Message<byte[]> bytesFromKafkaToAmq(Message<byte[]> kafkaBytesMessage) {
    Log.info("bytesFromKafkaToAmq triggered");
    return kafkaBytesMessage;
  }

  @Incoming("dead-letter-topic-kafka-to-amq-json-in")
  public CompletionStage<Void> processDLQ1(Message<String> rejected){
    Log.info("DLQ for dead-letter-topic-kafka-to-amq-json-in called");
    IncomingKafkaRecordMetadata<String, String> metadata = rejected.getMetadata(IncomingKafkaRecordMetadata.class)
            .orElseThrow(() -> new IllegalArgumentException("Expected a message coming from Kafka"));
    String reason = new String(metadata.getHeaders().lastHeader("dead-letter-reason").value());
    Log.infof("The message '%s' has been rejected and sent to the DLT. The reason is: '%s'.", rejected.getPayload(), reason);

    return rejected.ack();
  }

}
