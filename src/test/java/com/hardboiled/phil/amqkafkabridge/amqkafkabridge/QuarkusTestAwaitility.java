package com.hardboiled.phil.amqkafkabridge.amqkafkabridge;

import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

import java.time.Duration;

public interface QuarkusTestAwaitility {
    Duration POLL_DELAY = Duration.ofMillis(1000);
    Duration POLL_INTERVAL = Duration.ofMillis(2000);
    Duration TIMEOUT = Duration.ofMillis(10000);

    default ConditionFactory await(String alias) {
        return Awaitility.await(alias).pollDelay(POLL_DELAY).pollInterval(POLL_INTERVAL).timeout(TIMEOUT);
    }
}
