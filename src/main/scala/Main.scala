import actors.SupervisorActor
import akka.actor.{ActorRef, ActorSystem}
import akka.event.Logging
import akka.util.Timeout
import utils.{Config, InitHelper}

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object Main extends App with Config {
  implicit val actorSystem = ActorSystem("blablacar-actor-system")
  implicit val executionContext = actorSystem.dispatcher
  implicit val timeout = Timeout(5 seconds)

  val log = Logging.getLogger(actorSystem, this)

  val filename = appConfig.getString("inputFilename")
  val src = Source.fromFile(filename)
  val lines = src.getLines()
  val gridInfoLine = lines.next()

  val (topRightCornerX, topRightCornerY) = InitHelper.getGridInfo(gridInfoLine)
  val width = topRightCornerX+1
  val height = topRightCornerY+1

  val mowers: Queue[ActorRef] = InitHelper.readLines(lines, width, height)
  log.info(s"supervisor added ${mowers.length} mowers")

  val supervisorActor = actorSystem.actorOf(SupervisorActor.props(mowers, width+1, height+1))
  supervisorActor ! SupervisorActor.InitializeMowers
  src.close()
}
