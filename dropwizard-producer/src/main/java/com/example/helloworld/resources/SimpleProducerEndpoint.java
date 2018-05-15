package com.example.helloworld.resources;

import com.example.helloworld.core.Person;
import com.example.helloworld.db.PersonDAO;
import com.example.helloworld.views.PersonView;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;


import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Properties;

@Path("/producer/{personName}")
@Produces(MediaType.APPLICATION_JSON)
public class SimpleProducerEndpoint {


    @GET
    @UnitOfWork
    public void getPerson(@PathParam("personName") String name) {

        System.out.println("producer");
        Properties properties=new Properties();
        //kafka bootstrpa server
        properties.setProperty("bootstrap.servers","127.0.0.1:9092");
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer",StringSerializer.class.getName());
        //producer acks
        properties.setProperty("acks","1");
        properties.setProperty("retries","3");
        properties.setProperty("linger.ms","1");

        Producer<String,String> producer=
                new org.apache.kafka.clients.producer.KafkaProducer<String,String>(properties);
            ProducerRecord<String,String> producerRecord=
                    new ProducerRecord<String, String>("totopic",name);
            producer.send(producerRecord);
        //producer.flush();
        producer.close();




    }


}
