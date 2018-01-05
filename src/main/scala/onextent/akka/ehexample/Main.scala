package onextent.akka.ehexample

import akka.stream.scaladsl.Source
import onextent.akka.eventhubs.Conf._
import onextent.akka.eventhubs.{Eventhubs, InputEventHubConf1, InputEventHubConf2}

object Main extends App {

  val sourceGraph = new Eventhubs(InputEventHubConf1)
 // val sourceGraph2 = new Eventhubs(InputEventHubConf2)

  val mySource = Source.fromGraph(sourceGraph)
  //val mySource2 = Source.fromGraph(sourceGraph2)

  mySource.runForeach(m => {
    println(s"source1: ${m._1.substring(0, 160)}")
    m._2.ack()
  })

//  mySource2.runForeach(m => {
//    println(s"source2: ${m._1.substring(0, 160)}")
//    m._2.ack()
//  })

  /*
  val restartSource = RestartSource.withBackoff(
    minBackoff = 3.seconds,
    maxBackoff = 30.seconds,
    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
  ) { () =>
    // Create a source from a future of a source
    mySource
  }

  //mySource.runForeach(m => {
  restartSource.runForeach(m => {
    val printme = m._1.length match {
      case l if l <= 160 => m._1
      case _ => m._1.substring(0, 160)
    }
    println(s"ejs yay: $printme")
    m._2.ack()
  })
   */

}
