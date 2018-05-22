package com.example.helloworld;

import com.example.Customer;
import com.example.helloworld.kafkaConsumers.SimpleConsumer;
import com.example.helloworld.kafkaConsumers.TestConsumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

@Ignore
public class ConsumerTest {

    MockConsumer<String, String> consumer;

    @Before
    public void setUp() {
        consumer = new MockConsumer<String, String>(OffsetResetStrategy.EARLIEST);
    }

    @Test
    public void testConsumer() throws IOException {
        // This is YOUR consumer object
        TestConsumer myTestConsumer = new TestConsumer();
        // Inject the MockConsumer into your consumer
        // instead of using a KafkaConsumer
        myTestConsumer.consumer = consumer;

        consumer.assign(Arrays.asList(new TopicPartition("my_topic", 0)));

        HashMap<TopicPartition, Long> beginningOffsets = new HashMap<>();
        beginningOffsets.put(new TopicPartition("my_topic", 0), 0L);
        consumer.updateBeginningOffsets(beginningOffsets);

        consumer.addRecord(new ConsumerRecord<String, String>("my_topic",
            0, 0L, "mykey", "myvalue0"));
        consumer.addRecord(new ConsumerRecord<String, String>("my_topic", 0,
            1L, "mykey", "myvalue1"));
        consumer.addRecord(new ConsumerRecord<String, String>("my_topic", 0,
            2L, "mykey", "myvalue2"));
        consumer.addRecord(new ConsumerRecord<String, String>("my_topic", 0,
            3L, "mykey", "myvalue3"));
        consumer.addRecord(new ConsumerRecord<String, String>("my_topic", 0,
            4L, "mykey", "myvalue4"));

        // This is where you run YOUR consumer's code
        // This code will consume from the Consumer and do your logic on it
        myTestConsumer.consume();

        // This just tests for exceptions
        // Somehow test what happens with the consume()
    }





}
