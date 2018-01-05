package onextent.akka.ehexample

import scala.concurrent.duration._
import akka.stream.scaladsl.{RestartSource, Source}
import onextent.akka.eventhubs._
import onextent.akka.eventhubs.Conf._

object Main extends App {

  val sourceGraph = new Eventhubs()

  val mySource = Source.fromGraph(sourceGraph)

  mySource.runForeach(m => {
    println(s"ejs yay: ${m._1.substring(0, 160)}")
    m._2.ack()
  })

//  val restartSource = RestartSource.withBackoff(
//    minBackoff = 3.seconds,
//    maxBackoff = 30.seconds,
//    randomFactor = 0.2 // adds 20% "noise" to vary the intervals slightly
//  ) { () =>
//    // Create a source from a future of a source
//    mySource
//  }
//
//
//  //mySource.runForeach(m => {
//  restartSource.runForeach(m => {
//    val printme = m._1.length match {
//      case l if l <= 160 => m._1
//      case _ => m._1.substring(0, 160)
//    }
//    println(s"ejs yay: $printme")
//    m._2.ack()
//  })

}
