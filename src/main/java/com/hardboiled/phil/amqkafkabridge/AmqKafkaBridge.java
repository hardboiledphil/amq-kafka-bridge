package com.hardboiled.phil.amqkafkabridge;

import static java.time.temporal.ChronoUnit.SECONDS;

import io.quarkus.logging.Log;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

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
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS)
  public Message<String> jsonFromAmqToKafka(Message<String> amqJsonMessage) {
    Log.info("jsonFromAmqToKafka triggered");
    return amqJsonMessage;
  }

  @Incoming("kafka-to-amq-json-in") // Kafka Topic
  @Outgoing("kafka-to-amq-json-out") // AMQ queue
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS)
  public Message<String> jsonFromKafkaToAmq(Message<String> kafkaJsonMessage) {
    Log.info("jsonFromKafkaToAmq triggered");
    return kafkaJsonMessage;
  }

  @Incoming("amq-to-kafka-bytes-in") // AMQ queue
  @Outgoing("amq-to-kafka-bytes-out") // Kafka topic
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS)
  public Message<byte[]> bytesFromAmqToKafka(Message<byte[]> amqBytesMessage) {
    Log.info("bytesFromAmqToKafka triggered");
    return amqBytesMessage;
  }

  @Incoming("kafka-to-amq-bytes-in") // kafka topic
  @Outgoing("kafka-to-amq-bytes-out") // AMQ queue
  @Retry(delay = 5, maxRetries = 5, delayUnit = SECONDS)
  public Message<byte[]> bytesFromKafkaToAmq(Message<byte[]> kafkaBytesMessage) {
    Log.info("bytesFromKafkaToAmq triggered");
    return kafkaBytesMessage;
  }

}
