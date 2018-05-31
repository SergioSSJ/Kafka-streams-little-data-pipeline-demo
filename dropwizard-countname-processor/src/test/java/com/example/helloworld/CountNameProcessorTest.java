package com.example.helloworld;

import com.example.Customer;
import com.example.helloworld.kafka.EmbeddedSingleNodeKafkaCluster;
import com.example.helloworld.kafkaProcessors.CountNameProcessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.*;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.test.ProcessorTopologyTestDriver;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;

public class CountNameProcessorTest {

    private Customer customer;



    @ClassRule
    public static final EmbeddedSingleNodeKafkaCluster CLUSTER = new EmbeddedSingleNodeKafkaCluster();
    private static JsonNode json;

    @BeforeClass
    public static void createTopics() {
        CLUSTER.createTopic("topictest");
    }

    @BeforeClass
    static public void readCustomerAvrosConsumerFile() throws IOException {
        YAMLFactory f = new YAMLFactory();
        f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        ObjectMapper mapper = new ObjectMapper(f);
        json = mapper.readTree(new File("customer-avros-consumer.yml"));
    }

    @Before
    public void initCustomer() {
        customer = Customer.newBuilder()
            .setAge(34)
            .setAutomatedEmail(false)
            .setFirstName("dumas")
            .setLastName("Doe")
            .setHeight(178f)
            .setWeight(75f)
            .setHey("heyhou")
            .build();
    }

    @Test
    public void shouldRunTheWikipediaFeedExample() throws Exception {
        Customer customer = Customer.newBuilder()
            .setAge(34)
            .setAutomatedEmail(false)
            .setFirstName("dumas")
            .setLastName("Doe")
            .setHeight(178f)
            .setWeight(75f)
            .setHey("heyhou")
            .build();

        final Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, CLUSTER.schemaRegistryUrl());

        final KafkaProducer<String, Customer> producer = new KafkaProducer<>(props);
        producer.send(new ProducerRecord<>("topictest", customer));
        producer.flush();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", CLUSTER.bootstrapServers());
        properties.setProperty("group.id", json.get("group.id").textValue());
        properties.setProperty("auto.commit.enable", json.get("auto.commit.enable").toString());
        properties.setProperty("auto.offset.reset", json.get("auto.offset.reset").textValue());
        properties.put("schema.registry.url", CLUSTER.schemaRegistryUrl());
        properties.setProperty("key.deserializer", json.get("key.deserializer").textValue());
        properties.setProperty("value.deserializer", json.get("value.deserializer").textValue());
        properties.setProperty("specific.avro.reader", json.get("specific.avro.reader").toString());

        KafkaConsumer<String, Customer> consumer = new KafkaConsumer<>(properties);
        String topic = "topictest";
        consumer.subscribe(Collections.singleton(topic));
        String expected = "dumas";
        String actual = "";

        final long timeout = System.currentTimeMillis() + 5000L;
        while (actual != expected && System.currentTimeMillis() < timeout) {
            final ConsumerRecords<String, Customer> records = consumer.poll(1000);
            for (ConsumerRecord<String, Customer> record : records) {
                actual = record.value().getFirstName();
                System.out.println("record<>: "+record.value());
            }
        }
        assertThat(expected, equalTo(actual));
    }
    @Test
    public void countNameProcessorTopologyTest(){
        Customer customer = Customer.newBuilder()
            .setAge(34)
            .setAutomatedEmail(false)
            .setFirstName("dumas")
            .setLastName("Doe")
            .setHeight(178f)
            .setWeight(75f)
            .setHey("heyhou")
            .build();

        final Properties propsProducer = new Properties();
        propsProducer.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers());
        propsProducer.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propsProducer.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        propsProducer.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, CLUSTER.schemaRegistryUrl());

        final KafkaProducer<String, Customer> producer = new KafkaProducer<>(propsProducer);
        producer.send(new ProducerRecord<>("customer-avros", customer));
        producer.flush();

        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount");
        props.put(StreamsConfig.CLIENT_ID_CONFIG, "wordcount-avro");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, CLUSTER.bootstrapServers() );
        props.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, CLUSTER.schemaRegistryUrl());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streamss");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,  1000);
        StreamsConfig config = new StreamsConfig(props);

        CountNameProcessor countNameProcessor=new CountNameProcessor();
        Topology topology=countNameProcessor.getTopology();
        KafkaStreams stream= new KafkaStreams(topology, config);

        String inputTopic = "customer-avros";
        String outputTopic = "customer-avros-count";
        stream.start();

        Properties properties = new Properties();
        properties.put("bootstrap.servers", CLUSTER.bootstrapServers());
        properties.setProperty("group.id", "otrooo");
        properties.setProperty("auto.commit.enable", json.get("auto.commit.enable").toString());
        properties.setProperty("auto.offset.reset", json.get("auto.offset.reset").textValue());
        properties.put("schema.registry.url", CLUSTER.schemaRegistryUrl());
        properties.setProperty("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        properties.setProperty("value.deserializer", "org.apache.kafka.common.serialization.LongDeserializer");
        properties.setProperty("specific.avro.reader", json.get("specific.avro.reader").toString());
        KafkaConsumer<String, Long> consumer = new KafkaConsumer<>(properties);
        String topic = "customer-avros-count";
        consumer.subscribe(Collections.singleton(topic));
        Long expected = 1L;
        Long actual = 0L;

        final long timeout = System.currentTimeMillis() + 10000L;
        while (actual != expected && System.currentTimeMillis() < timeout) {
            final ConsumerRecords<String,Long> records = consumer.poll(1000);
            for (ConsumerRecord<String, Long> record : records) {
                actual = record.value();
                System.out.println("recordd"+record);
            }
        }

        assertThat(expected, equalTo(actual));
    }










}
