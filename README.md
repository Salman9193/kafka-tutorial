# kafka-tutorial

Use cases :

	Messaging System

	Activity Tracking

	Gather metrics from many different location

	Application logs gathering

	Stream processing (Apache Kafka Streams API or Apache Spark)

	De-coupling of system dependencies

	Integration with Spark, Flink , Storm , Hadoop, and many other Big Data Technologies
    

Examples : 

	Netflix : recommendations in real-time while user watches TV shows

	Uber : to gather user,taxi, trip data in real time to compute and forecast demand and compute surge pricing in real time

	LinkedIn : Prevent Spam ,collect user interactions to make better connection recommendations in real time


# Kafka Theory : 
	
	Topics , Partitions and Offsets : 

		Topics : particular stream of Data 
				Similar to a table in a database

		Topics are split in partitions

			Kafka Topic -> Partition 0 , Partition 1 , Partition 2
			Each partition is ordered 
			Each message within a partition gets an incremental id, called offset

		Offset only have a meaning for a specific Partition 
		Order is kept only within a partition
		Data is kept only for a limited time (default is one week)
		Once data is written to a partition , it cant be changed (immutability)
		Data is assigned randomly to a partition in case a key is not provided

	Brokers : 
		A Kafka Cluster is composed of multiple brokers (server)
		Each broker is identified by an id
		Each Broker contains certain topics partitions
		After connecting to any broker (certain bootstrap server), you will be connected to the entire cluster

	Brokers and Topics : 
		Topic-A with 3 partitions
		Topic-B with 2 partitions

			Broker 101					Broker 102						Broker 103

		Topic-A/Partition-0			Topic-A/Partition-2				Topic-A/Partition-1

		Topic-B/Partition-1			Topic-B/Partition-0

		Note : Data is distributed and Broker 103 does not have any Topic B data


	Topic Replication Factor :

		Topics should have a replication factor > 1 (usually taken between 2 and 3)
		This way if a broker is down , another broker can serve the data
		Example , Topic-A with replication factor of 2 and 2 partition

			Broker 101 					Broker 102						Broker 103
		Topic-A/Partition-0			Topic-A/Partition-1

									Topic-A/Partition-0				Topic-A/Partition-1

		Concept of  a leader in a partition: 
			At any time: one broker can be a leader for a a given partition
			Only that leader can receive and serve data for the partition
			Other brokers will only synchronize the data
			Therefore each partition will have one leader and multiple ISRs (in-sync replicas)

	Producers :
		Producer write data to topics (which is made of topics)
		Producers automatically know which broker and partition to write to
		In case of broker failure, producers will automatically recover
		Producers can choose to receive acknowledgement of the data  writes : 
			1. acks=0 , wont wait for ack (data loss possibility)
			2. acks=1 (default) , producer will wait for leader acknowledgement (limited data loss)
			3. acks=all , leader + replicas acknowledgement (no data loss)

		Message keys : 
			Producer can choose to send key with the message
			if key=null , data is send in round robin 
			if key is sent, it will always go to the same partition

	Consumers : 
		Consumer reads data from the topic
		Consumers know which broker to read from 
		In case of broker failure , consumers know which broker to recover
		Data is read in order within each partitions

		Consumer Groups  : 
			Consumer read data in consumer groups
			Each consumer in a group read from exclusive partitions
			If you have more consumers than partition, some consumers will be inactive

	Consumer Offsets:
		Kafka stores the offsets at which the consumer group has been reading
		offsets committed live in a kafka topic named __consumer__offsets
		When a consumer in a group has processed data received from kafka , it should be commiting the offsets
		if a consumer dies, it will be able to read back from where it left off thanks to the committed consumer offsets

		Delivery Semantics : 
			Consumers choose when to commit offset
			There are 3 delivery semantics: 
				1. At most once : offsets are committed as soon as the message is received , if processing goes wrong the message is lost (it wont be read again) 
				2. At least once (usually preferred): offsets are committed after the message has been processed , if processing goes wrong , it will be read again (need to maintain idempotency)
				3. Exactly once : can be achieved for Kafka => Kafka workflows (Kafka Streams API), For kafka => External WorkFlow Systems 	, use idempotent consumer

	Kafka Broker Discovery : 
		Every kafka broker is called a bootstrap server
		That means you can connect to one kafka broker and you will be connected to entire cluster
		Each broker knows about all the broker, topics and partitions (metadata)

	Zookeeper: 
		Zookeeper manages brokers (keeps a list of them)
		helps in performing leader elections for partitions
		sends notification to kafka in case of any changes (new topic, broker dies , broker comes up, delete topics)

		Kafka cant work without Zookeeer
		Zookeeper by design operates with odd number of servers (3,5,7)
		Zookeeper has a leader (handle writes) rest of the servers are the followers (handle reads)
		Zookeeper does not store the consumer offsets with Kafka
		

Application : 

	Read tweets from Twitter api => Twitter Producer => Kafka Broker => Consume messages from the topic => ingest in database/elastic search

Twitter Application :

	Twitter Producer : 

		Twitter hose bird client :  https://github.com/twitter/hbc 



Producer Configurations : 

	acks : 
	
	0,1,all
	
	0 : no ack is considered by producer
	
	1 : ack from only leader is considered from producer (partial durability)
	
	all : acks from leader and  in-sync replicas (durability)

	min.insync.replicas

Idempotent Producers:

	The idempotent producer feature addresses the acks issues ensuring that messages always get delivered, in the right order and without duplicates.

	Limitation 1: Acks=All

		You can choose not to set acks at all, and by using enable.idempotence=true, the acks configuration will automatically be set to all for you.

	Limitation 2: max.in.flight.requests.per.connection <= 5

		You must either leave max.in.flight.requests.per.connection (alias max.in.flight) left unset so that it be automatically set for you or manually set it to 5 or less. If you leave it explicitly set to a value higher than 5 you’ll get error.

Compression : 

	Producer usually send data that is text-based, for example with JSON data

	In this case , it is important to apply compression to the producer.

	Compression is enabled at the producer level and doesnt require andy configuration change in the broker or the the consumers

	("compression,type") Compression type can be "none" (default), "gzip", "lz4", "snappy"

	Compression is more effective the bigger the batch of message being sent to Kafka !

	Link : benchmark : https://blog.cloudflare.com/squeezing-the-firehose/


Message compression : 

	Compressed batch has the following advantage : 

		Much smaller producer request size ( compression ratio upto 4x)
		Faster to transfer data over the network => less latency
		Better throughput
		Better disk utilization in Kafka ( stored messages on disk are smaller )

	Disadvantages :

		Producers must commit some CPU cycles to compression
		Consumers must commit some CPU cycles to decompression

	Overall:

		Consider testing snappy or lz4 for optimal speed/compression ratio

	 Find acompresion allgorith that gives you the best performance for your specific data. Test all of them

	 Consider tweaking linger.ms and batch.size to have bigger batches, and therefore more compression and higher throughput


	 linger.ms and batch.size : 

	 	Number of ms a producer is willing to wait before sending a batch out (default 0)

	 	By introducing some lag , we increase the chances of sending messages together in a batch

	 	So at the expense of introducing a small delay, we can increase throughput, compression and efficiency of our producer

	 	batch.size : Maximum number of bytes that will be included in a batch. The default is 16 Kb

	 	A batch is allocated per partition, so make sure that you dont set it to a number that is too high , otherwise you will run waste memory !

	 	average batch size metric can be monitored using Kafka Producer Metric 


# CLI

Zookeeper should be started

Kafka broker should be started


Topics : 

	kafka-topics / kafka-topics.sh:
		(create,delete,describe or change a topic)

	Create Topic (minimum required parameter options): 
		kafka-topics.sh --zookeeper 127.0.0.1:2181 --topic <topic-name> --create --partitions <num_partition> --replication-factor <num_replicas>
		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --topic <topic-name> --create --partitions <num_partition> --replication-factor <num_replicas>

		Note: Replication factor <= num of brokers

	List topics : 
		kafka-topics.sh --zookeeper 127.0.0.1:2181 --list
		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --list

		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --list
		Topic1
		Topic2
		Topic3


	Describe topic: 
		kafka-topics.sh --zookeeper 127.0.0.1:2181 --topic <topic-name> --describe
		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --topic <topic-name> --describe

		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --topic Topic1 --describe

		Topic: Topic1	TopicId: 17wZwLLmRhyF-rgpP_nYhg	PartitionCount: 3	ReplicationFactor: 1	Configs: segment.bytes=1073741824
			Topic: Topic1	Partition: 0	Leader: 0	Replicas: 0	Isr: 0
			Topic: Topic1	Partition: 1	Leader: 0	Replicas: 0	Isr: 0
			Topic: Topic1	Partition: 2	Leader: 0	Replicas: 0	Isr: 0

	Delete a topic:
		kafka-topics.sh --zookeeper 127.0.0.1:2181 --topic <topic-name> --delete
		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --topic <topic-name> --delete

		kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --delete --topic Topic3

Producers : 

	kafka-console-producer.sh  /  kafka-console-producer :
		(puts data to Kafka topics and outputs it to standard output

	kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic <topic-name>

	kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic Topic1
	>hello world!
	>

	kafka-console-producer.sh --broker-list 127.0.0.1:9092 --topic <topic-name> --producer-property acks=all

	Note : using the cli command for producing to a topic not created before, this will create the topic but the topic would have the default configuration speficied in the properties file

Consumers:

	kafka-console-consumer.sh / kafka-console-consumer : 
		(helps to read data from Kafka topics and outputs it to standard output.)

	kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic <topic-name> 
	kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic Topic1 --from-beginning

	group : 
	kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic Topic1 --group <group-name-1>
	kafka-console-consumer.sh --bootstrap-server 127.0.0.1:9092 --topic Topic1 --group <group-name-2>
	(partitions will be allocated to group accordingly -- round robin)


Consumer-Group : 

	kafka-consumer-groups.sh / kafka-consumer-groups :
	(list all consumer groups,describe a consumer-group, delete consumer-group info , or reset consumer-group offset)

	kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --list
		consumerApplication
		group33
		group-first
		group-second
		group2
		group1

	kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --describe --group consumerApplication   

		Consumer group 'consumerApplication' has no active members.

		GROUP               TOPIC           PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG             CONSUMER-ID     HOST            CLIENT-ID
		consumerApplication Topic1          0          60              61              1               -               -               -
		consumerApplication Topic1          1          98              98              0               -               -               -
		consumerApplication Topic1          2          40              40              0               -               -               -

	 Resetting Offset :

		kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --group consumerApplication --reset-offsets --to-earliest --topic <topic-name> --execute
		kafka-consumer-groups.sh --bootstrap-server 127.0.0.1:9092 --group consumerApplication --reset-offsets --shift-by +/-<num_offset_to_shift> --topic <topic-name> --execute

Producer with keys :

	kafka-console-producer --broker-list 127.0.0.1:9092 --topic first_topic --property parse.key=true --property key.separator=,
		> key,value
		> another key,another value

Consumer with keys : 

	kafka-console-consumer --bootstrap-server 127.0.0.1:9092 --topic first_topic --from-beginning --property print.key=true --property key.separator=,


# Tools : 

	KafkaCat (https://github.com/edenhill/kafkacat) is an open-source alternative to using the Kafka CLI, created by Magnus Edenhill.
	https://medium.com/@coderunner/debugging-with-kafkacat-df7851d21968

	Conduktor : https://www.conduktor.io/
	Conduktor allows you to perform all the administrative tasks on Kafka (such as creating topics, partitions, etc), as well as produce and consume
	
	
# Delivery Semantics : 

	At most once : 
		
		Offsets are committed as soon as the message batch is received. If the processing goes wrong , the message will be lost. (it wont be read again)

	At least once : 

		Offsets are committed after the message is processed. if processing goes wrong, the message will be read again.This can result in duplicate processing of messages.
		Make sure the processing of the message is idempotent (processing of the same message again would not cause any impact)

	Exactly once : 

		Kafka -> Kafka workflows (Streams API)


Consumer Poll Behaviour:
	
	Kafka Consumers have a poll model while many other messaging bus in enterprises have a push mechanism model

	allows how the consumer want to control where in the log they want to consume and how fast and gives the ability to replay the messages/events

	fetch.min.bytes : 

		controls how much data you want to pull atleast on each request.

		Helps improving the throughput and decreasing the request number

		at the cost of latency

	max.poll.records : (500 by default)

		controls how many records to receive per poll request

		increase if your messages are small and have a lot of available RAM

		Good to monitor how many records are polled per request

	max.partitions.fetch.bytes : (default 1 MB) 

		Maximum data returned by broker by broker per partition

		if you read from 100 partitions, you will need a lot of memory (RAM)

	fetch.max.bytes : (default 50 MB)

		Maximum data returned for each fetch request (covers multiple partition)

		The consumer performs multiple fetches in parallel

Consumer Offset commit Stratefies :

	1. enable.auto.commit=true : syncronous process of batching

	2. enable.auto.commit=false : manually commit of offsets

Consumer internal threads : 

	session.timeout.ms : (default 10 seconds)

		Heartbeats sent periodically to the broker

		if no heartbeat is sent during that period, the consumer is considered to be dead

		Set even lower timeout session to consumer rebalances


	heartbeat.interval.ms : (default 3 ms)

		How often to send heartbeat

		usually send to 1/3rd of session.timeout.ms 

	max.poll.interval.ms : (default 5 minutes) 

		Maximum amount of time between two .poll() calls before declaring the consumer dead

		particularly relevant for Big Data framework like Spark in case the processing takes time

