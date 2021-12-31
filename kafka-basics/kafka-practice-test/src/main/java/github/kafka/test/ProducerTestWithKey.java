package github.kafka.test;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ProducerTestWithKey {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(ProducerTestWithKey.class);
        //create producer properties
        Properties prop = new Properties();
        prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
        prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
        //create producer
        KafkaProducer<String,String> producer = new KafkaProducer<String, String>(prop);

        //produce and send data
        int i = 0;
        while(i<10){
            String key = "key_id_" + Integer.toString(i);
            ProducerRecord<String,String> producerRecord = new ProducerRecord<>("Topic1", key,"second message " + i);
            producer.send(producerRecord,(recordMetadata, e) -> {
                if(e == null){
                    logger.info("recordMetadata sent : " +
                            "\n key : " + key +
                            "\n Topic : " + recordMetadata.topic()  +
                            "\n Partition : " + recordMetadata.partition() +
                            "\n Offset : " + recordMetadata.offset() +
                            "\n Timestamp : " + recordMetadata.timestamp());

                }else{
                    logger.error("Error while producing data : ",e);
                }
            });
            i++;
        }


        producer.flush();
        producer.close();
    }
}
