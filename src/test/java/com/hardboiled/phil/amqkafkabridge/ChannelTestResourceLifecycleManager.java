package com.hardboiled.phil.amqkafkabridge;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;

import java.util.HashMap;
import java.util.Map;

public class ChannelTestResourceLifecycleManager implements QuarkusTestResourceLifecycleManager {

  @Override
  public Map<String, String> start() {

    HashMap<String, String> channelProperties = new HashMap<>();
    channelProperties.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("amq-to-kafka-json-in"));
    channelProperties.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("amq-to-kafka-json-out"));

    channelProperties.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("kafka-to-amq-json-in"));
    channelProperties.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("kafka-to-amq-json-out"));

    channelProperties.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("amq-to-kafka-bytes-in"));
    channelProperties.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("amq-to-kafka-bytes-out"));

    channelProperties.putAll(InMemoryConnector.switchIncomingChannelsToInMemory("kafka-to-amq-bytes-in"));
    channelProperties.putAll(InMemoryConnector.switchOutgoingChannelsToInMemory("kafka-to-amq-bytes-out"));

    return channelProperties;
  }

  @Override
  public void stop() {
    InMemoryConnector.clear();
  }
}
