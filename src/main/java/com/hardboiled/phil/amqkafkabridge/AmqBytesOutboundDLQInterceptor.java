//package com.hardboiled.phil.amqkafkabridge;
//
//import io.quarkus.logging.Log;
//import io.smallrye.common.annotation.Identifier;
//import io.smallrye.reactive.messaging.OutgoingInterceptor;
//import jakarta.enterprise.context.ApplicationScoped;
//import org.eclipse.microprofile.reactive.messaging.Message;
//
//@Identifier("DLQ_TOPIC")
//@ApplicationScoped
//public class AmqBytesOutboundDLQInterceptor implements OutgoingInterceptor {
//
//
//    @Override
//    public Message<?> onMessage(Message<?> message) {
//        Log.info("INTERCEPTOR DLQ onMessage triggered");
//        return OutgoingInterceptor.super.onMessage(message);
//    }
//
//    @Override
//    public void onMessageAck(Message<?> message) {
//        Log.info("INTERCEPTOR DLQ onMessageAck triggered");
//    }
//
//    @Override
//    public void onMessageNack(Message<?> message, Throwable failure) {
//        Log.info("INTERCEPTOR DLQ onMessageNack triggered");
//    }
//}
