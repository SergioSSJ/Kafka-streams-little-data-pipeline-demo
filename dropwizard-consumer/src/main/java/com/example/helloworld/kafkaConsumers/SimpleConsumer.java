package com.example.helloworld.kafkaConsumers;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;
import com.example.Customer;


public class SimpleConsumer implements Managed {
/*
        Properties properties=new Properties();
        //kafka bootstrpa server
        properties.setProperty("bootstrap.servers","127.0.0.1:9092");
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer",StringDeserializer.class.getName());
        properties.setProperty("group.id","groupo11");
        properties.setProperty("enable.auto.commit","true");
        properties.setProperty("auto.commit.interval.ms","1000");
        properties.setProperty("auto.offset.reset","earliest");

        KafkaConsumer<String,String> kafkaConsumer=
                new KafkaConsumer<String, String>(properties);
        kafkaConsumer.subscribe(Arrays.asList("second_topic"));

        while(true){

            ConsumerRecords<String,String> consumerRecords=
                    kafkaConsumer.poll(100);
            for(ConsumerRecord<String,String> consumerRecord:consumerRecords){
                System.out.println(
                        "partiotion: "+ consumerRecord.partition()+
                        " ,value: "+consumerRecord.value()+
                        " ,topic: "+consumerRecord.topic());
            }
        }
 */



    @Override
    public void start(){

        Properties properties = new Properties();
        // normal consumer
        properties.setProperty("bootstrap.servers","127.0.0.1:9092");
        properties.put("group.id", "customer-consumer-group-v1");
        properties.put("auto.commit.enable", "false");
        properties.put("auto.offset.reset", "earliest");

        // avro part (deserializer)
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", KafkaAvroDeserializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");
        properties.setProperty("specific.avro.reader", "true");

        KafkaConsumer<String, Customer> kafkaConsumer = new KafkaConsumer<>(properties);
        String topic = "customer-avros";
        kafkaConsumer.subscribe(Collections.singleton(topic));

        System.out.println("Waiting for data...");

        while (true){
            System.out.println("Polling");
            ConsumerRecords<String, Customer> records = kafkaConsumer.poll(100);

            for (ConsumerRecord<String, Customer> record : records){
                Customer customer = record.value();
                System.out.println(customer);
            }

            kafkaConsumer.commitSync();
        }
    }

    @Override
    public void stop(){


    }






}
