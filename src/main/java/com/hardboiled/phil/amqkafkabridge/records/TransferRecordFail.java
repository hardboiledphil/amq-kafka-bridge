package com.hardboiled.phil.amqkafkabridge.records;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TransferRecordFail(
        @JsonProperty("stuffGoesHereX") String stuffGoesHereX
) { }
