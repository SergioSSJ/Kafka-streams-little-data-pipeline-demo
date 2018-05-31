package com.example.helloworld.kafkaConsumers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

import com.example.Customer;


public class SimpleConsumer implements Managed {


    private KafkaConsumer<String, Customer> consumer;
    private JsonNode jsonNode;

    public SimpleConsumer() throws IOException {
        YAMLFactory f = new YAMLFactory();
        f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        ObjectMapper mapper = new ObjectMapper(f);
        jsonNode = mapper.readTree(new File("customer-avros-consumer.yml"));
    }


    @Override
    public void start() {
        Properties properties = new Properties();
        properties.put("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("group.id", jsonNode.get("group.id").textValue());
        properties.setProperty("auto.commit.enable", jsonNode.get("auto.commit.enable").toString());
        properties.setProperty("auto.offset.reset", jsonNode.get("auto.offset.reset").textValue());
        properties.put("schema.registry.url", "http://127.0.0.1:8081");
        properties.setProperty("key.deserializer", jsonNode.get("key.deserializer").textValue());
        properties.setProperty("value.deserializer", jsonNode.get("value.deserializer").textValue());
        properties.setProperty("specific.avro.reader", jsonNode.get("specific.avro.reader").toString());

        KafkaConsumer<String, Customer> kafkaConsumer = new KafkaConsumer<>(properties);
        String topic = "customer-avros";
        kafkaConsumer.subscribe(Collections.singleton(topic));

        System.out.println("Waiting for data...");

        while (true) {
            System.out.println("Polling");
            ConsumerRecords<String, Customer> records = kafkaConsumer.poll(1000);

            for (ConsumerRecord<String, Customer> record : records) {
                System.out.println(record.value());
            }

            kafkaConsumer.commitSync();
        }
    }

    @Override
    public void stop() {


    }


}
