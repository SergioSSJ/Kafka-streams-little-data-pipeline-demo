package com.example.helloworld.kafkaConsumers;

import com.example.Customer;
import com.example.CustomerEnriched;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Collections;
import java.util.Properties;

public class JoinedConsumer implements Managed {


    @Override
    public void start(){

        Properties properties = new Properties();
        // normal consumer
        properties.setProperty("bootstrap.servers","127.0.0.1:9092");
        properties.put("group.id", "customer-consumer-groupo");
        properties.put("auto.commit.enable", "false");
        properties.put("auto.offset.reset", "earliest");
        // avro part (deserializer)
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");
        properties.setProperty("specific.avro.reader", "true");

        KafkaConsumer<String, CustomerEnriched> kafkaConsumer = new KafkaConsumer<>(properties);
        String topic = "joined";
        kafkaConsumer.subscribe(Collections.singleton(topic));

        System.out.println("Waiting for data...");

        while (true){
            System.out.println("Polling");
            ConsumerRecords<String, CustomerEnriched> records = kafkaConsumer.poll(1000);

            for (ConsumerRecord<String, CustomerEnriched> record : records){
                System.out.println("JOINED TOPIC: ");
                CustomerEnriched customer = record.value();
                System.out.println(customer);
            }

            kafkaConsumer.commitSync();
        }
    }

    @Override
    public void stop(){


    }














}
