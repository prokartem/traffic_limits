import Database.Postgres
import Entities._
import cats.effect.{ExitCode, IO, IOApp}

import collection.JavaConverters._
import org.slf4j.LoggerFactory
import com.typesafe.scalalogging._

object Main extends IOApp {

  private val logger = Logger(LoggerFactory.getLogger(this.getClass))

  val postgres = new Postgres[IO]
  val spark    = new SparkStreaming(5)
  val kafka    = new Kafka(spark)

  override def run(args: List[String]): IO[ExitCode] = for {
    _ <- postgres.migrate
    _ =
      if (args.length == 1)
        println(
          s"DB migrated! Start counting packets with src ${args.mkString(" ")}",
        )
      else
        println(
          s"DB migrated! Start counting packets without filter",
        )
    _ = kafka.admin.createTopics(List(kafka.topic).asJava)
    _ = spark
      .customReceiverStream(args)
      .reduce(_ + _)
      .map(l =>
        for {
          min <- postgres.getMinLimit
          max <- postgres.getMaxLimit
          _ = println(s"$l is in [$min, $max]?...")
          _ = kafka.checkLimits(l, min, max)
        } yield (),
      )
      .foreachRDD(_.foreach(_.unsafeRunSync()))

    minKafkaStream = kafka.kafkaStream("min")
    maxKafkaStream = kafka.kafkaStream("max")

    _ = List(minKafkaStream, maxKafkaStream)
      .foreach(
        _.map(msg =>
          for {
            _ <- postgres.insertNewLimit(msg.key, msg.value)
            _ = println(s"sent to db new limit: ${msg.key} -> ${msg.value}")
          } yield (),
        ).foreachRDD(_.foreach(_.unsafeRunSync())),
      )
    _ = spark.ssc.start()
    _ = spark.ssc.awaitTermination()
  } yield ExitCode.Success
}
