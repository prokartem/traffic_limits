package Entities

import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Duration, Minutes, Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.ReceiverInputDStream

class SparkStreaming(minutes: Int) {
  val batchInterval: Duration = Minutes(minutes)
  private val conf: SparkConf =
    new SparkConf().setMaster("local[*]").setAppName("TrafficCountWithAlerts")
  val ssc = new StreamingContext(conf, batchInterval)
  def customReceiverStream(args: List[String]): ReceiverInputDStream[Long] =
    ssc.receiverStream(new CustomReceiver(StorageLevel.MEMORY_AND_DISK_2, args))
}
