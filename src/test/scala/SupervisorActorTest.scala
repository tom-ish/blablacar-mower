import actors.MowerActor.{FinalPosition, MakeMove}
import actors.{MowerActor, SupervisorActor}
import actors.SupervisorActor.{CollectFinalPosition, NewPositionEvent, NextMove}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import model.Orientation.{E, N, W}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.immutable.Queue

class SupervisorActorTest extends TestKit(ActorSystem("MowerSupervisorTestSystem"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  val width = 6
  val height = 6

  "A SupervisorActor" should {
    "send a MakeMove message" in {
      // given
      val x1 = 1
      val y1 = 2
      val d1 = N
      val moves1 = "LF"
      val mower1 = system.actorOf(MowerActor.props(
        x1, y1, d1,
        moves1,
        width, height
      ))

      val x2 = 0
      val y2 = 2
      val d2 = E
      val moves2 = "FR"
      val mower2 = system.actorOf(MowerActor.props(
        x2, y2, d2,
        moves2,
        width, height
      ))

      val queue = Queue(mower1, mower2)
      val supervisorActor = system.actorOf(SupervisorActor.props(queue, width, height))

      // when
      supervisorActor ! NextMove(queue)
      supervisorActor ! CollectFinalPosition
      mower1 ! MowerActor.FinalPosition

      // then
    }
  }
}
