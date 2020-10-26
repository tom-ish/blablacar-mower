package actors

import actors.MowerActor._
import akka.actor.{Actor, ActorLogging, Props}
import model.Orientation._
import model.{Command, F, L, R}

object MowerActor {
  sealed trait MowerEvent
  case class InitMoves(moves: String) extends MowerEvent
  case object InitialPosition extends MowerEvent
  case object MakeMove extends MowerEvent
  case object MoveValid extends MowerEvent
  case object CollisionDetected extends MowerEvent
  case object MoveNotValid extends MowerEvent
  case object FinalPosition extends MowerEvent
  case class FinalPosition(finalX: Int, finalY: Int, finalO: Direction) extends MowerEvent

  def props(x: Int, y: Int, d: Direction, moves: String, gridWidth: Int, gridHeight: Int) = Props(new MowerActor(x, y, d, moves, gridWidth, gridHeight))
}

class MowerActor(var x: Int, var y: Int, var d: Direction, initialMoves: String, gridWidth: Int, gridHeight: Int) extends Actor with ActorLogging {
  import context._

  override def receive: Receive = initialization

  /**
   * We wait for the Supervisor to ask for our initial position and then change state
   */
  def initialization: Receive = {
    case InitialPosition =>
      sender ! SupervisorActor.InitializeMowerPosition(x, y)
      become(readyForMove(initialMoves))
  }

  /**
   * We wait for the Supervisor to ask for more moves:
   *  - if the moves sequence is empty, we noptify back the supervisor
   *  - otherwise, we send the updates to the supervisor to confirm or discard the positions, depending on collisions
   */
  def readyForMove(moves: String): Receive = {
    case MakeMove =>
      moves.headOption match {
        case Some(nextMove) =>
          nextMove match {
            case 'F' =>
              // Move valid: ask supervisor if move forward is allowed
              val (newX, newY) = computeNewPosition(x, y, d)
              become(waitingMoveValidation(newX, newY, moves))
              sender ! SupervisorActor.NewPositionEvent((x, y), (newX, newY))
            case 'L' =>
              // Move valid: update our own orientation to the left
              d = computeNewDirection(d, L)
              become(waitingMoveValidation(x, y, moves))
              // Notify supervisor that we made a move by sending an empty valid move
              sender ! SupervisorActor.NewOrientationEvent
            case 'R' =>
              // Move valid: update our own orientation to the right
              d = computeNewDirection(d, R)
              become(waitingMoveValidation(x, y, moves))
              // Notify supervisor that we made a move by sending an empty valid move
              sender ! SupervisorActor.NewOrientationEvent
            case _ =>
              // Move not valid: notify supervisor that move is not readable
              sender ! SupervisorActor.MoveNotValidEvent
          }
        case None =>
          become(finishedMove)
          sender ! SupervisorActor.NoMoreMoveEvent
      }
  }

  /**
   * we wait for the Supervisor the validation of the previously sent positions
   */
  def waitingMoveValidation(waitingX: Int, waitingY: Int, moves: String): Receive = {
    // Receive validation from supervisor: update position
    case MoveValid =>
      x = waitingX
      y = waitingY
      become(readyForMove(moves.drop(1)))
    // Receive Collision detection from the supervisor: discard the move
    case CollisionDetected =>
      become(readyForMove(moves.drop(1)))
    // Move not valid: we assume the strategy here is to discard the unreadable move
    case MoveNotValid =>
      become(readyForMove(moves.drop(1)))
  }


  /**
   * We answer back to the Supervisor our final position and direction.
   **/
  def finishedMove: Receive = {
    case FinalPosition =>
      sender ! FinalPosition(x, y, d)
  }

  private def computeNewPosition(x: Int, y: Int, direction: Direction): (Int, Int) = {
    direction match {
      case N => (x, ((y+1) + gridHeight) % gridHeight)
      case S => (x, ((y-1) + gridHeight) % gridHeight)
      case W => (((x-1) + gridWidth) % gridWidth, y)
      case E => (((x+1) + gridWidth) % gridWidth, y)
      case UnknownDirection => (x, y)
    }
  }

  private def computeNewDirection(direction: Direction, command: Command): Direction = {
    command match {
      case L =>
        direction match {
          case N => W
          case S => E
          case E => N
          case W => S
          case UnknownDirection => UnknownDirection
        }
      case R =>
        direction match {
          case N => E
          case S => W
          case E => S
          case W => N
          case UnknownDirection => UnknownDirection
        }
      case F => // should never be called
        direction
    }
  }
}
