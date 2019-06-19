package onextent.akka.ehexample

import akka.Done
import akka.actor.ActorSystem
import akka.pattern.AskTimeoutException
import akka.serialization.SerializationExtension
import akka.stream.scaladsl.Source
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object Conf extends LazyLogging {

  val conf: Config = ConfigFactory.load()
  implicit val actorSystem: ActorSystem = ActorSystem("example", conf)
  SerializationExtension(actorSystem)

  val decider: Supervision.Decider = {

    case _: AskTimeoutException =>
      // might want to try harder, retry w/backoff if the actor is really supposed to be there
      logger.warn(s"decider discarding message to resume processing")
      //Supervision.Restart
      Supervision.Stop

    case e: java.text.ParseException =>
      logger.warn(
        s"decider discarding unparseable message to resume processing: $e")
      Supervision.Resume

    case e =>
      logger.error(s"decider can not decide: $e")
      //Supervision.Restart
      Supervision.Stop

  }

  lazy val partitionId: Int = {
    sys.env.get("POD_NAME") match {
      case Some(str) =>
        val pos = str.lastIndexOf('-')
        str.substring(pos + 1).toInt
      case _ => throw new java.lang.IllegalArgumentException("no pid in POD_NAME")
    }
  }

  implicit val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))


  implicit def ec: ExecutionContext = ExecutionContext.global

  def handleTermination(src: Source[Any, Any]): Unit = {
    src.watchTermination() {
      (_, done) => done.onComplete {
        case Success(_) =>
          logger.warn("success on src. but stream should not end!")
          sys.exit(0)
        case Failure(ex) =>
          logger.error(s"failure on src. stream should not end! $ex", ex)
          sys.exit(1)
      }
    }
  }

  def handleTermination(result: Future[Done]): Unit = {
    result onComplete {
      case Success(_) =>
        logger.warn("success. but stream should not end!")
        actorSystem.terminate()
        sys.exit(0)
      case Failure(e) =>
        logger.error(s"failure. stream should not end! $e", e)
        actorSystem.terminate()
        sys.exit(-1)
    }
  }

}

