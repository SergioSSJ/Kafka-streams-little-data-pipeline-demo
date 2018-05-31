/*
 * Copyright Confluent Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.helloworld;

import com.example.Customer;
import com.example.helloworld.kafka.EmbeddedSingleNodeKafkaCluster;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class AvroProducerConsumerTest {

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

        final long timeout = System.currentTimeMillis() + 30000L;
        while (actual != expected && System.currentTimeMillis() < timeout) {
            final ConsumerRecords<String, Customer> records = consumer.poll(1000);
            for (ConsumerRecord<String, Customer> record : records) {
                actual = record.value().getFirstName();
            }
        }

        assertThat(expected, equalTo(actual));

    }

}
