package Entities

import org.apache.spark.internal.Logging
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver
import org.pcap4j.core.BpfProgram.BpfCompileMode
import org.pcap4j.core.PcapNetworkInterface.PromiscuousMode
import org.pcap4j.core.{PacketListener, PcapNetworkInterface, Pcaps}
import org.pcap4j.packet.Packet

class CustomReceiver(storageLevel: StorageLevel, args: List[String])
    extends Receiver[Long](storageLevel)
    with Logging {
  override def onStart(): Unit = new Thread("Pcap Receiver") {
    override def run(): Unit = receive
  }.start()

  override def onStop(): Unit = {}

  private def receive: Unit = {
    val nif: PcapNetworkInterface = Pcaps.findAllDevs().get(0)
    val snapLen: Int              = 65536
    val mode: PromiscuousMode     = PromiscuousMode.PROMISCUOUS
    val timeout: Int              = 10
    val handle                    = nif.openLive(snapLen, mode, timeout)

    if (args.length == 1)
      handle.setFilter(s"src ${args.head}", BpfCompileMode.OPTIMIZE)

    while (!isStopped) {
      val listener = new PacketListener {
        override def gotPacket(packet: Packet): Unit =
          store(packet.getRawData.length.toLong)
      }
      handle.loop(10000000, listener)
    }

    handle.close()

    restart("RESTART")
  }

}
