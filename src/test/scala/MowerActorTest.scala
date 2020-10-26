import actors.{MowerActor, SupervisorActor}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import model.Orientation.N
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

class MowerActorTest extends TestKit(ActorSystem("MowerTestSystem"))
  with ImplicitSender
  with AnyWordSpecLike
  with Matchers
  with BeforeAndAfterAll {
  override protected def afterAll(): Unit = TestKit.shutdownActorSystem(system)

  // given
  val gridWidth = 6
  val gridHeight = 6

  "A Mower actor" must {
    "send back a NewOrientationEvent message when move = L" in {
      // given
      val x = 1
      val y = 2
      val direction = N
      val moves = "L"
      val mower = system.actorOf(
        MowerActor.props(
          x, y, direction,
          moves,
          gridWidth, gridHeight
        ))

      // when
      mower ! MowerActor.MakeMove

      // then
      expectMsg(SupervisorActor.NewOrientationEvent)
    }

    "send back a NewOrientationEvent message when move = R" in {
      // given
      val x = 1
      val y = 2
      val direction = N
      val moves = "R"
      val mower = system.actorOf(
        MowerActor.props(
          x, y, direction,
          moves,
          gridWidth, gridHeight
        ))

      // when
      mower ! MowerActor.MakeMove

      // then
      expectMsg(SupervisorActor.NewOrientationEvent)
    }

    "send back a NewPositionEvent message when move = F" in {
      // given
      val x = 1
      val y = 2
      val direction = N
      val moves = "F"
      val mower = system.actorOf(
        MowerActor.props(
          x, y, direction,
          moves,
          gridWidth, gridHeight
        ))

      // when
      mower ! MowerActor.MakeMove

      // then
      expectMsg(SupervisorActor.NewPositionEvent(
        previousPosition = (1, 2),
        newPosition = (1, 3)
      ))
    }

    "send back a MoveNotValidEvent message when move = _" in {
      // given
      val x = 1
      val y = 2
      val direction = N
      val moves = "A"
      val mower = system.actorOf(
        MowerActor.props(
          x, y, direction,
          moves,
          gridWidth, gridHeight
        ))

      // when
      mower ! MowerActor.MakeMove

      // then
      expectMsg(SupervisorActor.MoveNotValidEvent)
    }
  }
}
