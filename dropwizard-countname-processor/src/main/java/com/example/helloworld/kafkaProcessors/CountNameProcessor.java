package com.example.helloworld.kafkaProcessors;

import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.dropwizard.lifecycle.Managed;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;

import java.sql.Time;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import com.example.Customer;

public class CountNameProcessor implements Managed {
    /*
    kafka-console-consumer --bootstrap-server localhost:9092 \
>     --topic customer-avros-count \
>     --from-beginning \
>     --formatter kafka.tools.DefaultMessageFormatter \
>     --property print.key=true \
>     --property print.value=true \
>     --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer \
>     --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
     */

    @Override
    public void start() throws Exception {
        final Properties streamsConfiguration = new Properties();
        // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
        // against which the application is run.
        streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount");
        streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "wordcount-avro");
        // Where to find Kafka broker(s).
        streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        // Where to find the Confluent schema registry instance(s)
        streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");
        // Specify default (de)serializers for record keys and for record values.
        streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streamss");
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
        final KTable<Windowed<String>, Long> aggregated = feeds
            // map the user id as key
            .map((key, value) -> {
                System.out.println("key: "+key+" value: "+value);
                return new KeyValue<>(value.getFirstName(), value);
            })
            // no need to specify explicit serdes because the resulting key and value types match our default serde settings
            .groupByKey().windowedBy(TimeWindows.of(5000))
            .count();

        // write to the result topic, need to override serdes
        KStream<String,Long> windowedStream=aggregated.toStream((key,value)->{
            System.out.println("windowed string to string suena la campana: "+key );
            return key.key();
        });
        windowedStream.to("customer-avros-count", Produced.with(stringSerde, longSerde));
        //aggregated.toStream().to("customer-avros-count", Produced.with(stringSerde, longSerde));
        KafkaStreams stream= new KafkaStreams(builder.build(), streamsConfiguration);
        stream.start();
    }

    @Override
    public void stop() throws Exception {

    }
}
