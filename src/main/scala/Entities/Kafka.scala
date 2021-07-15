package Entities

import org.apache.kafka.clients.admin.{Admin, NewTopic}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.{
  LongDeserializer,
  LongSerializer,
  StringDeserializer,
  StringSerializer,
}
import org.apache.spark.streaming.Seconds
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.KafkaUtils
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent

import scala.collection.JavaConverters._

class Kafka(spark: SparkStreaming) {
  private val kafkaParams = Map[String, Object](
    "bootstrap.servers"     -> "localhost:9092",
    "acks"                  -> "all",
    "key.serializer"        -> classOf[StringSerializer],
    "value.serializer"      -> classOf[LongSerializer],
    "heartbeat.interval.ms" -> "10000",
    "session.timeout.ms"    -> "10000",
  )
  private val kafkaParamsOUT = Map[String, Object](
    "bootstrap.servers"  -> "localhost:9092",
    "key.deserializer"   -> classOf[StringDeserializer],
    "value.deserializer" -> classOf[LongDeserializer],
    "group.id"           -> "use_a_separate_group_id_for_each_stream",
    "auto.offset.reset"  -> "latest",
    "enable.auto.commit" -> (false: java.lang.Boolean),
  )
  private val adminParams = Map[String, Object](
    "bootstrap.servers"       -> "localhost:9092",
    "connections.max.idle.ms" -> "10000",
  )
  private val producer: KafkaProducer[String, Long] =
    new KafkaProducer[String, Long](kafkaParams.asJava)
  private def record(
      limitName: String,
      limitValue: Long,
  ): ProducerRecord[String, Long] =
    new ProducerRecord[String, Long]("alerts", limitName, limitValue)

  val admin: Admin    = Admin.create(adminParams.asJava)
  val topic: NewTopic = new NewTopic("alerts", 1, 3.shortValue())

  def checkLimits(value: Long, min: Long, max: Long): Unit =
    if (value < min) producer.send(record("min", value))
    else if (value > max) producer.send(record("max", value))
    else println(s"$value is ok")

  def kafkaStream(limitName: String): DStream[LimitMsg] =
    KafkaUtils
      .createDirectStream[String, Long](
        spark.ssc,
        PreferConsistent,
        Subscribe[String, Long](
          List(topic.name()).asJavaCollection,
          kafkaParamsOUT.asJava,
        ),
      )
      .map(rec => LimitMsg(rec.key(), rec.value()))
      .window(spark.batchInterval.*(4), spark.batchInterval.*(4))
      .filter(_.key == limitName)
      .reduce((x1, x2) =>
        limitName match {
          case "min" =>
            if (x1.value < x2.value) x1
            else x2
          case "max" =>
            if (x1.value > x2.value) x1
            else x2
        },
      )
}
