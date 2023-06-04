package com.hardboiled.phil.amqkafkabridge.records;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferRecord(
        @JsonProperty("stuffGoesHere") String stuffGoesHere
) { }
