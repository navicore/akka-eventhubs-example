package onextent.akka.ehexample

import akka.stream.scaladsl.Source
import com.typesafe.config.{Config, ConfigFactory}
import onextent.akka.eventhubs.{EventHubConf, Eventhubs}
import Conf._

object Main extends App {
  val cfg1: Config = ConfigFactory.load().getConfig("eventhubs-1")
  val sourceGraph1 = new Eventhubs(EventHubConf(cfg1))
  val mySource1 = Source.fromGraph(sourceGraph1)
  mySource1.runForeach(m => {
    println(s"source1: ${m._1.substring(0, 160)}")
    m._2.ack()
  })

}
