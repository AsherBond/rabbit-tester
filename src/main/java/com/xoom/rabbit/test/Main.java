/*
The MIT License (MIT)

Copyright (c) 2013 Xoom Corporation

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package com.xoom.rabbit.test;

import java.util.Random;
import java.util.concurrent.CountDownLatch;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;

public class Main {

    private static final String QUEUE_NAME = "xoomQueue";
    private static final String EXCHANGE_NAME = "xoomExchange";
    private static final String ROUTING_KEY = "my.routing.key";
    private static final Random RND = new Random();

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 9) {
            System.out.println("usage: java -jar target/rabbit-tester-0.1-SNAPSHOT-standalone.jar [consumer_threads] [number_of_messages] [amqp_host] [amqp_port] [produce] [consume] [message size in bytes] [username] [password]");
            return;
        }
        final long startTime = System.currentTimeMillis();
        int consumerThreads = Integer.parseInt(args[0]);
        final int messages = Integer.parseInt(args[1]);
        String host = args[2];
        int port = Integer.parseInt(args[3]);
        boolean produce = Boolean.parseBoolean(args[4]);
        boolean consume = Boolean.parseBoolean(args[5]);
        final int messageSize = Integer.parseInt(args[6]);
        String username = args[7];
        String password = args[8];

        if (produce) {
            System.out.println("Sending " + messages + " messages to " + host + ":" + port);
        }
        if (consume) {
            System.out.println("Consuming " + messages + " messages from " + host + ":" + port);
        }
        if (!produce && !consume) {
            System.out.println("Not producing or consuming any messages.");
        }

        CachingConnectionFactory connectionFactory = new CachingConnectionFactory(host, port);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        connectionFactory.setChannelCacheSize(consumerThreads + 1);

        RabbitAdmin amqpAdmin = new RabbitAdmin(connectionFactory);

        DirectExchange exchange = new DirectExchange(EXCHANGE_NAME, true, false);
        Queue queue = new Queue(QUEUE_NAME);
        amqpAdmin.declareExchange(exchange);
        amqpAdmin.declareQueue(queue);
        amqpAdmin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));

        final AmqpTemplate amqpTemplate = new RabbitTemplate(connectionFactory);

        final CountDownLatch producerLatch = new CountDownLatch(messages);
        final CountDownLatch consumerLatch = new CountDownLatch(messages);

        SimpleMessageListenerContainer listenerContainer = null;

        if (consume) {
            listenerContainer = new SimpleMessageListenerContainer();
            listenerContainer.setConnectionFactory(connectionFactory);
            listenerContainer.setQueueNames(QUEUE_NAME);
            listenerContainer.setConcurrentConsumers(consumerThreads);
            listenerContainer.setMessageListener(new MessageListener() {
                @Override public void onMessage(Message message) {
                    if (consumerLatch.getCount() == 1) {
                        System.out.println("Finished consuming " + messages + " messages in " + (System.currentTimeMillis() - startTime) + "ms");
                    }
                    consumerLatch.countDown();
                }
            });
            listenerContainer.start();
        }

        if (produce) {
            while (producerLatch.getCount() > 0) {
                try {
                    byte[] message = new byte[messageSize];
                    RND.nextBytes(message);
                    amqpTemplate.send(EXCHANGE_NAME, ROUTING_KEY, new Message(message, new MessageProperties()));
                    producerLatch.countDown();
                } catch (Exception e) {
                    System.out.println("Failed to send message " + (messages - producerLatch.getCount()) + " will retry forever.");
                }
            }
        }

        if (consume) {
            consumerLatch.await();
            listenerContainer.shutdown();
        }

        connectionFactory.destroy();
    }
}
