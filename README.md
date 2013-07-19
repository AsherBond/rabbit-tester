rabbit-tester
=============

A utility to load test rabbitmq.

Build
-----------
mvn clean install

Usage
---------
    java -jar target/rabbit-tester-0.1-SNAPSHOT-standalone.jar [consumer_threads] [number_of_messages]
        [amqp_host] [amqp_port] [produce] [consume] [message_size_in_bytes] [username] [password]

Arguments
---------------

**consumer_threads**: the number of consumer threads to spawn

**number_of_messages**: the number of messages to publish and/or consume

**amqp_host**: the hostname of the rabbitmq (or other AMQP compliant) server

**amqp_port**: the port of the rabbitmq server

**produce**: a boolean to enable or disable producing messages from this test instance

**consume**: a boolean to enable or disable consuming messages from this test instance

**message_size in_bytes**: the size of each message body in bytes

**username**: the username for authenticating to the AMQP server

**password**: the credentials for authenticating to the AMQP server
