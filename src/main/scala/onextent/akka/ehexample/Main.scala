package onextent.akka.ehexample

import akka.stream.scaladsl.Source
import onextent.akka.eventhubs.Eventhubs
import onextent.akka.eventhubs.Conf._

object Main extends App {

  val sourceGraph = new Eventhubs

  val mySource = Source.fromGraph(sourceGraph)

  mySource.runForeach(m => {

    println(s"ejs yay: ${m._1.substring(0, 160)}")
    m._2.ack()
  })

}
