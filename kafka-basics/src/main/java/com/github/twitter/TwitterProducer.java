package com.github.twitter;

import com.google.common.collect.Lists;
import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Client;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.Hosts;
import com.twitter.hbc.core.HttpHosts;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.event.Event;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class TwitterProducer {
        //read from config file properties
        //TODO: fetch from vault
        private static String consumer_key = "oBwggHjNiZpETTy6uQfFs8ygZ";
        private static String consumer_secret = "qWxcX3eFIzu3oUG2SMlg0dcwsI2GhIKJnFUKmz6kJkWEP7VsNl";
        private static String token = "1210576135823585281-aSnJP6OgqCjBJ9bRwLB7ZkP7jvSyxT";
        private static String tokenSecret = "GHfVd37yX7fqtpa0QeS9RTbc338s4T6ktPaJ8bnRe6BzY";



        /** Set up your blocking queues: Be sure to size these properly based on expected TPS of your stream */
        private BlockingQueue<String> msgQueue;
        private BlockingQueue<Event> eventQueue;
        List<String> terms = Lists.newArrayList("twitter", "api");
        Logger logger = LoggerFactory.getLogger(TwitterProducer.class);
        Client twitterClient;
        String topicName;

        public TwitterProducer(){
            msgQueue = new LinkedBlockingQueue<String>(100000);
            eventQueue = new LinkedBlockingQueue<Event>(1000);
            topicName = "twitter_tweets";
        }

        public static void main(String[] args){
            new TwitterProducer().run();

        }

        public void run() {

            // create Twitter client
            twitterClient = createTwitterClient();

            //create a Kafka Producer
            KafkaProducer<String,String> kafkaProducer = getKafkaProducer();
            // Attempts to establish a connection.
            Runtime.getRuntime().addShutdownHook(new Thread(() ->{
                        logger.info("Stopping Twitter Producer ...");
                        logger.info("Stopping Kafka Producer ...");
                        kafkaProducer.close();
                        logger.info("Kafka Producer Stopped...");
                        logger.info("Stopping Twitter Client ...");
                        twitterClient.stop();
                        logger.info("Twitter Client Stopped ...");
                        logger.info("Twitter Producer stopped...");
                    })
            );

            twitterClient.connect();

            while (!twitterClient.isDone()) {
                try {
                    String msg = msgQueue.poll(5, TimeUnit.SECONDS);

                    String key = terms.stream().reduce((s, s2) -> s + "," + s2).get();
                    ProducerRecord<String,String> producerRecord = new ProducerRecord<>(topicName, key,msg);
                    kafkaProducer.send(producerRecord,(recordMetadata, e) -> {
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
                } catch (InterruptedException e) {
                    logger.info("Exception while polling message to the twitter client");
                    twitterClient.stop();
                }
            }




        }

        private Client createTwitterClient() {

            /** Declare the host you want to connect to, the endpoint, and authentication (basic auth or oauth) */
            Hosts hosebirdHosts = new HttpHosts(Constants.STREAM_HOST);
            StatusesFilterEndpoint hosebirdEndpoint = new StatusesFilterEndpoint();
            // Optional: set up some followings and track terms
    //        List<Long> followings = Lists.newArrayList(1234L, 566788L);
    //        hosebirdEndpoint.followings(followings);
            hosebirdEndpoint.trackTerms(terms);

            // These secrets should be read from a config file
            Authentication hosebirdAuth = new OAuth1( consumer_key, consumer_secret, token , tokenSecret);


            ClientBuilder builder = new ClientBuilder()
                    .name("Hosebird-Client-01")                              // optional: mainly for the logs
                    .hosts(hosebirdHosts)
                    .authentication(hosebirdAuth)
                    .endpoint(hosebirdEndpoint)
                    .processor(new StringDelimitedProcessor(msgQueue));
    //                .eventMessageQueue(eventMessageQueuetQueue);                          // optional: use this if you want to process client events

            Client hosebirdClient = builder.build();
            return hosebirdClient;

        }



        private  KafkaProducer<String,String> getKafkaProducer(){
            Properties prop = new Properties();
            prop.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"127.0.0.1:9092");
            prop.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            prop.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,StringSerializer.class.getName());
            //create producer
            KafkaProducer<String,String> producer = new KafkaProducer<String, String>(prop);
            return producer;
        }
}
