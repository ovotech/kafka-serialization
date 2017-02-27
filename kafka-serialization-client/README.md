## Kafka client

Lightweight, non-blocking wrappers around the Apache Kafka Consumer and Producer classes.

Consumer example:

```scala
import com.ovoenergy.serialization.kafka.client.consumer.KafkaConsumer
import com.typesafe.config.Config
import com.ovoenergy.serialization.kafka.client.consumer._
import com.ovoenergy.serialization.kafka.client.Topic
import scala.concurrent.Future

val config: Config = _
val name: ConsumerName =_
val clientId: ClientId = _
val topic: Topic = _

// At this stage, consumer is not yet consuming.
val consumer = KafkaConsumer[String, String](config, name, clientId, topic)

// Subscribe to pre-configured topics to start consuming.
consumer.subscribe { case event => 
    println(event.value)
    Future.successful((): Unit)
}

// Clean up after the work is done.
consumer.stop()
```

Producer example:

```scala
import com.ovoenergy.serialization.kafka.client.producer.KafkaProducer
import com.typesafe.config.Config
import com.ovoenergy.serialization.kafka.client.producer._
import com.ovoenergy.serialization.kafka.client.Topic

val config: Config = _
val name: ProducerName = _
val topic: Topic = _
val event = Event[String, String](topic, "key", "value")

// Start producer.
val producer = KafkaProducer[String, String](config, name)

// Send typed event.
producer.produce(event)


// Clean up after the work is done.
producer.stop()
```

For more details see [kafka integraion test](src/it/scala/com/ovoenergy/serialization/kafka/client/KafkaIntegrationSpec).

## Integration tests

To run integration tests, run `./bin/run_integration_tests.sh` from project root directory.