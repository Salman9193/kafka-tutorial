package com.github.twitter.consumer;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

public class ElasticSearchConsumer {

    static String  hostname = "kafka-course-5628145103.us-east-1.bonsaisearch.net";
    static String username = "l6eah4vxea";
    static String password = "bhw4hvwi5n";
    static Logger logger = LoggerFactory.getLogger(ElasticSearchConsumer.class);

    public static void main(String[] args) throws IOException {
        RestHighLevelClient client = createClient();

        //create consumer
        KafkaConsumer<String,String> consumer = createKafkaConsumer();

        //subscribe to topics
        consumer.subscribe(Collections.singleton("twitter_tweets"));

        //poll for new data
        while(true){
            ConsumerRecords<String,String> records = consumer.poll(Duration.ofMillis(100));
            logger.info("Received " + records.count() + " records ");
            if(records.count() > 0){
                BulkRequest bulkRequest = new BulkRequest();
                records.forEach(record -> {

                    try {
                        String jsonString = record.value();
                        String id = extractIdFromTweet(jsonString);
                        IndexRequest indexRequest = new IndexRequest("twitter", "tweets", id).source(jsonString, XContentType.JSON);
                        bulkRequest.add(indexRequest);
                    } catch (NullPointerException e){
                        logger.error("NullPointer Exception for the request (Bad Data Skipped)  : " + record.key(), e);
                    } catch (Exception e){
                        logger.error("InterruptedException for the request : " + record.key(), e);
                    }
                    logger.info("Key : " + record.key() +
                            "\n value : " + record.value() +
                            "\n partition : " + record.partition());

                });
                BulkResponse bulkResponse = client.bulk(bulkRequest,RequestOptions.DEFAULT);
                logger.info(bulkResponse.getItems().toString());
                logger.info("Committing the offset ...");
                consumer.commitSync();
                logger.info("Committed ...");
            }

        }
//        client.close();






    }

    private static String extractIdFromTweet(String jsonString) {
        JsonParser jsonParser = new JsonParser();
        return jsonParser.parse(jsonString)
                .getAsJsonObject()
                .get("id_str")
                .getAsString();
    }

    private static KafkaConsumer<String, String> createKafkaConsumer() {

        Properties properties = new Properties();
        properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"localhost:9092");
        properties.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.setProperty(ConsumerConfig.GROUP_ID_CONFIG,"twitter_tweets_elastic_search_app");
        properties.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        properties.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"false"); // to disable auto commit of offsets


        KafkaConsumer<String,String> consumer = new KafkaConsumer<String, String>(properties);

        return  consumer;
    }

    public static RestHighLevelClient createClient(){
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(username, password));
        RestClientBuilder builder = RestClient.builder(
                new HttpHost(hostname , 443, "https")).setHttpClientConfigCallback(new RestClientBuilder.HttpClientConfigCallback() {
                    @Override
                    public HttpAsyncClientBuilder customizeHttpClient(HttpAsyncClientBuilder httpAsyncClientBuilder) {
                        return httpAsyncClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                    }
        });
        RestHighLevelClient client = new RestHighLevelClient(builder);
        return client;
    }
}
