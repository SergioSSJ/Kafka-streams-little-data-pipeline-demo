package com.example.helloworld.kafkaProcessors;

import com.example.Customer;
import com.example.CustomerEnriched;
import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;

import java.util.Properties;

public class SimpleStreamsJoinDemo implements Managed {







    @Override
    public void start() throws Exception {
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-avro-lambda-examples");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "wordcount-avro-lambda-example-clients");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // Where to find the Confluent schema registry instance(s)
        streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://127.0.0.1:8081");
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams");
        streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Records should be flushed every 10 seconds. This is less than the default
        // in order to keep this example interactive.
        streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG,  1000);

        final Serde<String> stringSerde = Serdes.String();
        final Serde<Long> longSerde = Serdes.Long();

        final StreamsBuilder builder = new StreamsBuilder();

        // read the source stream
        final KStream<String,Customer> feeds = builder.stream("customer-avros");

        // aggregate the new feed counts of by user
        final KTable<String, Long> aggregated = feeds

            // map the user id as key
            .map((key, value) -> {
                System.out.println("key: "+key+" value: "+value);
                return new KeyValue<>(value.getFirstName(), value);
            })
            // no need to specify explicit serdes because the resulting key and value types match our default serde settings
            .groupByKey()//.windowedBy(TimeWindows.of(5000))
            .count();
        // write to the result topic, need to override serdes

        /*
        KStream<String,Long> windowedStream=aggregated.toStream((key,value)->{
            System.out.println("windowed string to string: "+key );
            return key.key();
        });*/
        //windowedStream.to("customer-avros-count", Produced.with(stringSerde, longSerde));
        //aggregated.toStream().to("customer-avros-count", Produced.with(stringSerde, longSerde));
        final KStream<String,Customer> feedsStreams = builder.stream("customer-avrosS");

        KStream<String,CustomerEnriched> joined=
            feeds.selectKey((k,v)->v.getFirstName())
                .join(aggregated,(v1,v2)-> CustomerEnriched.newBuilder()
                    .setAge(v1.getAge())
                    .setAutomatedEmail(v1.getAutomatedEmail())
                    .setCount(v2.intValue())
                    .setFirstName(v1.getFirstName())
                    .setAge(v1.getAge())
                    .setLastName(v1.getLastName())
                    .setHeight(v1.getHeight())
                    .setWeight(v1.getWeight())
                    .setHey(v1.getHey())
                    .build());
/*
    { "name": "first_name", "type": "string", "doc": "First Name of Customer" },
       { "name": "last_name", "type": "string", "doc": "Last Name of Customer" },
       { "name": "age", "type": "int", "doc": "Age at the time of registration" },
       { "name": "height", "type": "float", "doc": "Height at the time of registration in cm" },
       { "name": "weight", "type": "float", "doc": "Weight at the time of registration in kg" },
       { "name": "automated_email", "type": "boolean", "default": true, "doc": "Field indicating if the user is enrolled in marketing emails" },
       { "name": "hey", "type": "string", "doc": "hey doc" }
 */

        joined.to("joined");






        KafkaStreams stream= new KafkaStreams(builder.build(), streamsConfiguration);
        stream.start();

    }

    @Override
    public void stop() throws Exception {

    }
}
