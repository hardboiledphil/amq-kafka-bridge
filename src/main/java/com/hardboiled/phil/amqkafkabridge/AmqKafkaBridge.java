package com.hardboiled.phil.amqkafkabridge;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hardboiled.phil.amqkafkabridge.records.TransferRecord;
import io.quarkus.logging.Log;
import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment.Strategy;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import java.io.IOException;
import java.util.concurrent.CompletionStage;

/**
 * Main component used for bridging between Kafka and AMQ for GEM/EOD related message flows
 *
 * <p>Some of the flows are just XML messages and will be transferred as strings.
 * Some of the flows represent java objects and will aim to be proto based but are just byte[] for now
 */
@ApplicationScoped
public class AmqKafkaBridge {

  public static final String AMQ_BYTES_IN = "AMQ.BYTES.IN";
  public static final String AMQ_BYTES_OUT = "AMQ.BYTES.OUT";
  public static final String DLQ_QUEUE = "AMQ.DLQ";
  public static final String KAFKA_BYTES_OUT = "kafka-bytes-out";
  public static final String KAFKA_BYTES_IN = "kafka-bytes-in";
  public static final String DLQ_TOPIC = "DLQ_TOPIC";

  static ObjectMapper objectMapper;

  static {
    objectMapper = new ObjectMapper();
  }

  @Incoming("kafka-to-amq-bytes-in") // kafka topic
  @Outgoing("kafka-to-amq-bytes-out") // AMQ queue
  @Acknowledgment(Strategy.MANUAL)
  public Message<byte[]> bytesFromKafkaToAmq(Message<byte[]> kafkaBytesMessage) throws IOException {
    Log.info("bytesFromKafkaToAmq triggered");

    TransferRecord record;
    byte[] newRecordBytes;

    try {
      record = objectMapper.readValue(kafkaBytesMessage.getPayload(), TransferRecord.class);
      var newRecord = new TransferRecord(record.stuffGoesHere().concat("-processed-kafka-to-amq"));
      newRecordBytes = objectMapper.writeValueAsBytes(newRecord);
    } catch (IOException ioException) {
      Log.error("bytesFromKafkaToAmq Exception caught => ", ioException);
      kafkaBytesMessage.nack(ioException);
      return null;
    }
    return Message.of(newRecordBytes, kafkaBytesMessage::ack);
  }

  @Incoming("dlt-kafka-to-amq-bytes-in")
  public CompletionStage<Void> processDLT1(Message<String> rejected){
    Log.info("DLQ for dead-letter-topic-kafka-to-amq-bytes-in called");
    IncomingKafkaRecordMetadata<String, String> metadata = rejected.getMetadata(IncomingKafkaRecordMetadata.class)
            .orElseThrow(() -> new IllegalArgumentException("Expected a message coming from Kafka"));
    String reason = new String(metadata.getHeaders().lastHeader("dead-letter-reason").value());
    Log.infof("The message '%s' has been rejected and was sent to the DLT. The reason is: '%s'.", rejected.getPayload(), reason);
    return rejected.ack();
  }

  @Incoming("amq-to-kafka-bytes-in")
  @Outgoing("amq-to-kafka-bytes-out")
  @Acknowledgment(Strategy.MANUAL)
  public Message<byte[]> bytesFromAmqToKafka(Message<byte[]> amqBytesMessage) {
    Log.info("bytesFromAmqToKafka triggered");

    TransferRecord record;
    byte[] newRecordBytes;

    try {
      record = objectMapper.readValue(amqBytesMessage.getPayload(), TransferRecord.class);
      var newRecord = new TransferRecord(record.stuffGoesHere().concat("-processed-amq-to-kafka"));
      newRecordBytes = objectMapper.writeValueAsBytes(newRecord);
    } catch (IOException ioException) {
      Log.error("bytesFromKafkaToAmq Exception caught => ", ioException);
      amqBytesMessage.nack(ioException);
      return null;
    }
    return Message.of(newRecordBytes , amqBytesMessage::ack);
  }

  @Incoming("dlt-amq-to-kafka-bytes-in")
  public CompletionStage<Void> processDLT2(Message<String> rejected){
    Log.info("dlt-amq-to-kafka-bytes-in. DLT for dead-letter-topic-amq-to-kafka-json-in called");
    IncomingKafkaRecordMetadata<String, String> metadata = rejected.getMetadata(IncomingKafkaRecordMetadata.class)
            .orElseThrow(() -> new IllegalArgumentException("Expected a message coming from Amq"));
    String reason = new String(metadata.getHeaders().lastHeader("dead-letter-reason").value());
    Log.infof("dlt-amq-to-kafka-bytes-in. The message '%s' has been rejected and was sent to the DLT. The reason is: '%s'.", rejected.getPayload(), reason);
    return rejected.ack();
  }

}
