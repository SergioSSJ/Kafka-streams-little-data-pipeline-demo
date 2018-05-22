package com.example.helloworld;

import com.example.Customer;
import com.google.common.collect.Iterables;
import com.salesforce.kafka.test.KafkaTestServer;
import com.salesforce.kafka.test.junit5.SharedKafkaTestResource;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.StreamsConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test of SharedKafkaTestResource.
 *
 * This also serves as an example of how to use this library!
 */
class SharedKafkaTestResourceTest {
    private static final Logger logger = LoggerFactory.getLogger(SharedKafkaTestResourceTest.class);

    /**
     * We have a single embedded kafka server that gets started when this test class is initialized.
     *
     * It's automatically started before any methods are run.
     * It's automatically stopped after all of the tests are completed.
     *
     * This example we override the Kafka broker id to '12', but this serves as an example of how you
     * how you could override any Kafka broker property.
     */
    @RegisterExtension
    public static final SharedKafkaTestResource sharedKafkaTestResource = new SharedKafkaTestResource()
        .withBrokerProperty("broker.id", "1000");

    /**
     * Before every test, we generate a random topic name and create it within the embedded kafka server.
     * Each test can then be segmented run any tests against its own topic.
     */
    private String topicName;

    /**
     * This happens once before every test method.
     * Create a new empty namespace with randomly generated name.
     */
    @BeforeEach
    void beforeTest() {
        // Generate topic name
        topicName = getClass().getSimpleName() + Clock.systemUTC().millis();

        // Create topic with a single partition,
        // NOTE: This will create partition id 0, because partitions are indexed at 0 :)
        getKafkaTestServer().createTopic(topicName, 1);
        getKafkaTestServer().createTopic("customer-avros",1);
    }


    @Test
    void testProducer() throws InterruptedException {
        Customer customer = Customer.newBuilder()
            .setAge(34)
            .setAutomatedEmail(false)
            .setFirstName("dead")
            .setLastName("Doe")
            .setHeight(178f)
            .setWeight(75f)
            .setHey("heyhou")
            .build();
        final int partitionId = 0;

        // Define our message
        final String expectedKey = "my-key";
        final String expectedValue = "my test message";

        // Define the record we want to produce
        ProducerRecord<String, Customer> producerRecord = new ProducerRecord<>("customer-avros", partitionId, expectedKey, customer);

        KafkaProducer<String, String> producer1 = getKafkaTestServer().getKafkaProducer(StringSerializer.class, StringSerializer.class, new Properties());
        Properties properties=new Properties();
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");
        properties.setProperty("specific.avro.reader", "true");
        // Create a new producer
        KafkaProducer<String, Customer> producer = getKafkaTestServer().getKafkaProducer(StringSerializer.class, KafkaAvroSerializer.class, properties);

        // Produce it & wait for it to complete.
        Future<RecordMetadata> future = producer.send(producerRecord);
        producer.flush();
        while (!future.isDone()) {
            Thread.sleep(500L);
        }
        logger.info("Produce completed");

        // Close producer!
        producer.close();






    }


    /**
     * Test that KafkaServer works as expected!
     *
     * This also serves as a decent example of how to use the producer and consumer.
     */
    @Test
    void testProducerAndConsumer() throws Exception {
        final int partitionId = 0;

        // Define our message
        final String expectedKey = "my-key";
        final String expectedValue = "my test message";

        // Define the record we want to produce
        ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, partitionId, expectedKey, expectedValue);

        // Create a new producer
        KafkaProducer<String, String> producer = getKafkaTestServer().getKafkaProducer(StringSerializer.class, StringSerializer.class);

        // Produce it & wait for it to complete.
        Future<RecordMetadata> future = producer.send(producerRecord);
        producer.flush();
        while (!future.isDone()) {
            Thread.sleep(500L);
        }
        logger.info("Produce completed");

        // Close producer!
        producer.close();

        // Create consumer
        try (final KafkaConsumer<String, String> kafkaConsumer =
                 getKafkaTestServer().getKafkaConsumer(StringDeserializer.class, StringDeserializer.class)) {

            final List<TopicPartition> topicPartitionList = new ArrayList<>();
            for (final PartitionInfo partitionInfo : kafkaConsumer.partitionsFor(topicName)) {
                topicPartitionList.add(new TopicPartition(partitionInfo.topic(), partitionInfo.partition()));
            }
            kafkaConsumer.assign(topicPartitionList);
            kafkaConsumer.seekToBeginning(topicPartitionList);

            // Pull records from kafka, keep polling until we get nothing back
            ConsumerRecords<String, String> records;
            do {
                records = kafkaConsumer.poll(2000L);
                logger.info("Found {} records in kafka", records.count());
                for (ConsumerRecord<String, String> record : records) {
                    // Validate
                    assertEquals(expectedKey, record.key(), "Key matches expected");
                    assertEquals(expectedValue, record.value(), "value matches expected");
                }
            }
            while (!records.isEmpty());
        }
    }

    /**
     * Test if we create a topic more than once, no errors occur.
     */
    @Test
    void testCreatingTopicMultipleTimes() {
        final String myTopic = "myTopic";
        for (int creationCounter = 0; creationCounter < 5; creationCounter++) {
            getKafkaTestServer().createTopic(myTopic);
        }
        assertTrue(true, "Made it here!");
    }

    /**
     * Validate that broker Id was overridden correctly.
     */
    @Test
    void testBrokerIdOverride() throws ExecutionException, InterruptedException {
        try (final AdminClient adminClient = getKafkaTestServer().getAdminClient()) {
            final Collection<Node> nodes = adminClient.describeCluster().nodes().get();

            assertNotNull(nodes, "Sanity test, should not be null");
            assertEquals(1, nodes.size(), "Should have 1 entry");

            // Get details about our test broker/node
            final Node node = Iterables.get(nodes, 0);

            // Validate
            assertEquals(1000, node.id(), "Has expected overridden broker Id");
        }
    }

    /**
     * Simple accessor.
     */
    private KafkaTestServer getKafkaTestServer() {
        return sharedKafkaTestResource.getKafkaTestServer();
    }
}
