package com.hardboiled.phil.amqkafkabridge.testcontainers;

import org.testcontainers.containers.Network;

public class TestNetworkResource {

    private static final Network network = Network.newNetwork();

    public static Network getNetwork() {
        return network;
    }
}
