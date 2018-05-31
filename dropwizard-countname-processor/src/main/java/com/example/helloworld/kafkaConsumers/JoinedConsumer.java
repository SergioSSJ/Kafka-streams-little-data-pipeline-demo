package com.example.helloworld.kafkaConsumers;

import com.example.Customer;
import com.example.CustomerEnriched;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Properties;

public class JoinedConsumer implements Managed {


    private JsonNode jsonNode;

    public JoinedConsumer() throws IOException {
        YAMLFactory f = new YAMLFactory();
        f.isEnabled(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        ObjectMapper mapper = new ObjectMapper(f);
        jsonNode = mapper.readTree(new File("joined-consumer.yml"));
    }

    @Override
    public void start() {
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.put("group.id", jsonNode.get("group.id").textValue());
        properties.put("auto.commit.enable", jsonNode.get("auto.commit.enable").toString());
        properties.put("auto.offset.reset", jsonNode.get("auto.offset.reset").textValue());
        properties.setProperty("key.deserializer", jsonNode.get("key.deserializer").textValue());
        properties.setProperty("value.deserializer", jsonNode.get("value.deserializer").textValue());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");
        properties.setProperty("specific.avro.reader", jsonNode.get("specific.avro.reader").toString());

        KafkaConsumer<String, CustomerEnriched> kafkaConsumer = new KafkaConsumer<>(properties);
        String topic = "joined";
        kafkaConsumer.subscribe(Collections.singleton(topic));

        System.out.println("Waiting for data...");

        while (true) {
            System.out.println("Polling");
            ConsumerRecords<String, CustomerEnriched> records = kafkaConsumer.poll(1000);

            for (ConsumerRecord<String, CustomerEnriched> record : records) {
                CustomerEnriched customer = record.value();
            }
            kafkaConsumer.commitSync();
        }
    }

    @Override
    public void stop() {
    }


}
