package com.hardboiled.phil.amqkafkabridge;

import static java.time.temporal.ChronoUnit.SECONDS;

import io.quarkus.logging.Log;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

/**
 * Main component used for bridging between Kafka and AMQ for message flows
 *
 * <p>Some of the flows are just XML messages and will be transferred as strings.
 * Some of the flows represent java objects and will aim to be proto based but are just byte[] for now
 */
public class AmqKafkaBridge {

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
