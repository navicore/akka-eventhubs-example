package onextent.akka.ehexample

import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.{Done, NotUsed}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import onextent.akka.ehexample.Conf._
import onextent.akka.eventhubs.Connector.AckableOffset
import onextent.akka.eventhubs.{EventHubConf, EventhubsBatchSink, EventhubsSink, EventhubsSinkData}
import onextent.akka.eventhubs.Eventhubs._

import scala.concurrent.{ExecutionContext, Future}

object MultiPartitionExample extends LazyLogging {

  def apply(): Unit = {

    implicit def ec: ExecutionContext = ExecutionContext.global

    var count = 0

    val consumer: Sink[(String, AckableOffset), Future[Done]] =
      Sink.foreach(m => {
        m._2.ack()
        count = count + 1
        println(s"ack count $count")
      })

    //val toConsumer = createToConsumer(consumer)

    val cfg: Config = ConfigFactory.load().getConfig("eventhubs-in")

    for (pid <- 0 until EventHubConf(cfg).partitions) {

      val src: Source[(String, AckableOffset), NotUsed] =
        createPartitionSource(pid, cfg)

      val flow =
        Flow[(String, AckableOffset)].map((x: (String, AckableOffset)) => {
          if (conf.getBoolean("main.pretty") && x._1.charAt(0) == '{') {
            import org.json4s._
            import org.json4s.native.JsonMethods._
            val parsedJson: JValue = parse(x._1)
            println(
              s"consumer pid $pid received:\n${pretty(render(parsedJson))}")
          } else {
            println(s"consumer pid $pid received:\n${x._1}")
            //println(s"consumer pid $pid received:\n${x._2.ackme.partitionKey}")
          }

          x
        })

      //src.via(flow).runWith(toConsumer)

      val last = src.viaMat(flow)(Keep.both)
        .toMat(consumer)(Keep.right).run()

      //handleTermination(last)
      handleTermination(src)

    }
  }

}

object SinglePartitionExample {

  def apply(pid: Int): Unit = {

    val cfg: Config = ConfigFactory.load().getConfig("eventhubs-in")

    val source1 = createPartitionSource(pid, cfg)

    source1.runForeach(m => {
      println(s"SINGLE SOURCE PID $pid: ${m._1.substring(0, 160)}")
      m._2.ack()
    })

  }

}

object SourceSinkExample extends LazyLogging {

  def apply(): Unit = {

    val inConfig: Config = ConfigFactory.load().getConfig("eventhubs-in")
    val outConfig: Config = ConfigFactory.load().getConfig("eventhubs-out")

    for (pid <- 0 until EventHubConf(inConfig).partitions) {

      val src: Source[(String, AckableOffset), NotUsed] =
        createPartitionSource(pid, inConfig)

      val flow =
        Flow[(String, AckableOffset)].map((x: (String, AckableOffset)) => {
          if (conf.getBoolean("main.pretty") && x._1.charAt(0) == '{') {
            import org.json4s._
            import org.json4s.native.JsonMethods._
            val parsedJson: JValue = parse(x._1)
            println(
              s"consumer pid $pid received:\n${compact(render(parsedJson))}")
          } else {
            println(s"consumer pid $pid received:\n${x._1}")
          }
          x
        })

      val format = Flow[(String, AckableOffset)].map(
        (x: (String, AckableOffset)) =>
          // pass the payload, the partition key, props, and ackable
          EventhubsSinkData(x._1.getBytes("UTF8"),
                            Some(x._2.ackme.partitionKey),
                            Some(x._2.ackme.properties.toMap),
                            Some(x._2)))

      src
        .via(flow)
        .via(format)
        .recover {
          case e =>
            logger.error(s"recover op caught ${e.getMessage}", e)
            throw e
        }
        .grouped(10)
        .runWith(new EventhubsBatchSink(EventHubConf(outConfig), pid))

    }
  }

}

object SourceSinkSinglePartitionExample extends LazyLogging {

  def apply(): Unit = {

    val inConfig: Config = ConfigFactory.load().getConfig("eventhubs-in")
    val outConfig: Config = ConfigFactory.load().getConfig("eventhubs-out")

    val pid = partitionId

    val src: Source[(String, AckableOffset), NotUsed] =
      createPartitionSource(pid, inConfig)

    val flow =
      Flow[(String, AckableOffset)].map((x: (String, AckableOffset)) => {
        if (conf.getBoolean("main.pretty") && x._1.charAt(0) == '{') {
          import org.json4s._
          import org.json4s.native.JsonMethods._
          val parsedJson: JValue = parse(x._1)
          println(
            s"consumer pid $pid received:\n${compact(render(parsedJson))}")
        } else {
          println(s"consumer pid $pid received:\n${x._1}")
        }
        x
      })

    val format = Flow[(String, AckableOffset)].map(
      (x: (String, AckableOffset)) =>
        // pass the payload, the partition key, props, and ackable
        EventhubsSinkData(x._1.getBytes("UTF8"),
                          Some(x._2.ackme.partitionKey),
                          Some(x._2.ackme.properties.toMap),
                          Some(x._2)))

    src
      .via(flow)
      .via(format)
      .recover {
        case e =>
          logger.error(s"recover op caught ${e.getMessage}", e)
          throw e
      }
      .runWith(new EventhubsSink(EventHubConf(outConfig), partitionId))

  }

}

object Main extends App {

  sys.env.getOrElse("MODE", "DEFAULT") match {
    case "MULTI_PARTITION" => MultiPartitionExample()
    case "SINGLE_PARTITION" => SinglePartitionExample(partitionId)
    case "SOURCE_SINK" => SourceSinkExample()
    case _ => MultiPartitionExample() // log to console
  }

}
