package github.kafka.test;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

public class ConsumerGroupTest {


    public static void main(String[] args) {
        //create properties
        Logger logger = LoggerFactory.getLogger(ConsumerGroupTest.class);
        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"consumerApplication");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");

        //create consumer
        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);

        //subscribe to topics
//        consumer.subscribe(Collections.singleton("Topic1"));

        //assign
        TopicPartition partition = new TopicPartition("Topic1" , 0);
        long offsetToReadFrom = 15L;
        consumer.assign(Arrays.asList(partition));

        //seek
        consumer.seek(partition,offsetToReadFrom);


        //poll for new data
        while(true){
            ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(100));
            records.forEach(
                    record -> logger.info("Key : " + record.key() +
                            "\n value : " + record.value() +
                            "\n partition : " + record.partition())
            );

        }
    }
}
