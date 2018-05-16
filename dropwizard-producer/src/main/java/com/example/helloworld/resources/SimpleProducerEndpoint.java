package com.example.helloworld.resources;

import com.example.helloworld.core.Person;
import com.example.helloworld.db.PersonDAO;
import com.example.helloworld.views.PersonView;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Properties;

import com.example.Customer;

@Path("/producer/{personName}")
@Produces(MediaType.TEXT_PLAIN)
public class SimpleProducerEndpoint {


    @GET
    @UnitOfWork
    public String getPerson(@PathParam("personName") String name) {
        Properties properties = new Properties();
        // normal producer
        properties.setProperty("bootstrap.servers", "127.0.0.1:9092");
        properties.setProperty("acks", "all");
        properties.setProperty("retries", "10");
        // avro part
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", KafkaAvroSerializer.class.getName());
        properties.setProperty("schema.registry.url", "http://127.0.0.1:8081");

        Producer<String, Customer> producer = new KafkaProducer<String, Customer>(properties);

        String topic = "customer-avros";

        // copied from avro examples
        Customer customer = Customer.newBuilder()
            .setAge(34)
            .setAutomatedEmail(false)
            .setFirstName(name)
            .setLastName("Doe")
            .setHeight(178f)
            .setWeight(75f)
            .setHey("heyhou")
            .build();

        ProducerRecord<String, Customer> producerRecord = new ProducerRecord<String, Customer>(
            topic, customer
        );

        System.out.println(customer);
        producer.send(producerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata metadata, Exception exception) {
                if (exception == null) {
                    System.out.println(metadata);
                } else {
                    exception.printStackTrace();
                }
            }
        });

        producer.flush();
        producer.close();


        return "goota";


    }


}
