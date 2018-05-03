package onextent.akka.ehexample

import akka.stream.scaladsl.{Sink, Source}
import akka.{Done, NotUsed}
import com.typesafe.config.{Config, ConfigFactory}
import onextent.akka.ehexample.Conf._
import onextent.akka.eventhubs.Connector.AckableOffset
import onextent.akka.eventhubs.EventHubConf
import onextent.akka.eventhubs.Eventhubs._

import scala.concurrent.Future

object MultiPartitionExample {

  def apply(): Unit = {

    val consumer: Sink[(String, AckableOffset), Future[Done]] =
      Sink.foreach(m => {
        println(s"SUPER SOURCE: ${m._1.substring(0, 160)}")
        m._2.ack()
      })

    val toConsumer = createToConsumer(consumer)

    val cfg: Config = ConfigFactory.load().getConfig("eventhubs-1")

    for (pid <- 0 until  EventHubConf(cfg).partitions) {

      val src: Source[(String, AckableOffset), NotUsed] =
        createPartitionSource(pid, cfg)

      src.runWith(toConsumer)

    }
  }

}

object SinglePartitionExample {

  def apply(): Unit = {

    val cfg: Config = ConfigFactory.load().getConfig("eventhubs-1")

    val source1 = createPartitionSource(0, cfg)

    source1.runForeach(m => {
      println(s"SINGLE SOURCE: ${m._1.substring(0, 160)}")
      m._2.ack()
    })

  }

}

object Main extends App {

  // TODO: create a toFlow example

  MultiPartitionExample()
  //SinglePartitionExample()

}
