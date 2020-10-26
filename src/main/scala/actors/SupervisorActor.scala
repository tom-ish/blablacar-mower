package actors

import actors.MowerActor.{FinalPosition, InitialPosition}
import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Success

object SupervisorActor {
  sealed trait SupervisorEvent

  case class AddMower(mowerActor: ActorRef) extends SupervisorEvent

  case object InitializeMowers extends SupervisorEvent
  case class InitializeMowerPosition(x: Int, y: Int) extends SupervisorEvent

  case object NoMoreMower extends SupervisorEvent
  case class NextMove(mowers: Queue[ActorRef]) extends SupervisorEvent
  case object FinishMoves extends SupervisorEvent

  case object CollectFinalPosition extends SupervisorEvent

  case object ErrorWhileProcessing extends SupervisorEvent

  sealed trait MoveEvent extends SupervisorEvent
  case class NewPositionEvent(previousPosition: (Int, Int), newPosition: (Int, Int)) extends MoveEvent
  case object NewOrientationEvent extends MoveEvent
  case object MoveNotValidEvent extends MoveEvent
  case object NoMoreMoveEvent extends MoveEvent

  /**
   * Actor are created through Props
   */
  def props(mowers: Queue[ActorRef], width: Int, height: Int) = Props(new SupervisorActor(mowers, width, height))
}

class SupervisorActor(initialMowersQueue: Queue[ActorRef], width: Int, height: Int) extends Actor
  with ActorLogging {
  import SupervisorActor._
  import context._
  implicit val timeout = Timeout(3 seconds)

  // initialization of the grid to false to all indexes
  var grid = Array.ofDim[Boolean](width, height)
  for {
    i <- 1 until width
    j <- 1 until height
  } grid(i)(j) = false

  /**
   * the receive function is given by the Actor parent's inheritance.
   * All the messages are processed through this function.
   * We define different states that handles different type of messages.
   * The first state when this actor is created is to send back to the SupervisorActor
   * its initial position (that the Supervisor is unaware of)
   */
  override def receive: Receive = setMowersPosition(initialMowersQueue)

  /**
   * Whenever we send a message, we change the state to be able to receive
   * the response from the MowerActor that contains its initialPosition
   * We always send a message to the actor at the head position in the queue.
   * If the queue is empty, we can switch state to compute mower moves
   */
  def setMowersPosition(mowersQueue: Queue[ActorRef]): Receive = {
    case InitializeMowers =>
      mowersQueue.dequeueOption match {
        case Some((mowerActor, remainingMowers)) =>
          become(waitingForMowersInitialPosition(remainingMowers))
          mowerActor ! MowerActor.InitialPosition
        case None =>
          become(readyForMoves(grid))
          // this sends a NextMove message to itself
          self ! NextMove(initialMowersQueue)
      }
  }

  /**
   * After receiving mower's initial position, we update the grid and
   * switch state to initialize remaining mowers position
   */
  def waitingForMowersInitialPosition(remainingMowers: Queue[ActorRef]): Receive = {
    case InitializeMowerPosition(x, y) =>
      grid(x)(y) = true
      become(setMowersPosition(remainingMowers))
      self ! InitializeMowers
  }

  /**
   * The queue is given as a parameter of the message, otherwise the same logic applies here:
   *  - we ask the mower at the head position in the queue for a new move
   *  - we switch state to be able to receive the correct message from the mower
   *  - if the queue is empty, it means that all mowers have sent their moves: we can switch to the final state
   */
  def readyForMoves(grid: Array[Array[Boolean]]): Receive = {
    case NextMove(mowers) =>
      mowers.dequeueOption match {
        case Some((currentMower, remainingMowers)) =>
          become(waitingForMoves(currentMower, remainingMowers, grid))
          currentMower ! MowerActor.MakeMove
        case None =>
          become(readyToCollectFinalPosition(initialMowersQueue))
          self ! CollectFinalPosition
      }
  }

  /**
   * In this state, we are sure to receive message only from MowerActors
   * we could have use sender() instead of currentMower, this is exactly the same
   * because we are sure that sender() is a MowerActor
   * the MowerActor is notified about the validity of its move, and we proceed to
   * the next move by passing in the parameter the new Queue, enqueued with the current mower
   * if we haven't been notified that there was no more move to process for this mower
   **/
  def waitingForMoves(currentMower: ActorRef, remainingMowers: Queue[ActorRef], grid: Array[Array[Boolean]]): Receive = {
    case NewPositionEvent((previousX, previousY), (newX, newY)) =>
      // a mower is already present at this position
      if (grid(newX)(newY))
        currentMower ! MowerActor.CollisionDetected
      // otherwise, grid is updated
      else {
        grid(previousX)(previousY) = false
        grid(newX)(newY) = true
        currentMower ! MowerActor.MoveValid
      }
      become(readyForMoves(grid))
      self ! NextMove(remainingMowers :+ currentMower)

    // Mower updated its orientation: no update needed on the grid
    case NewOrientationEvent =>
      currentMower ! MowerActor.MoveValid
      become(readyForMoves(grid))
      self ! NextMove(remainingMowers :+ currentMower)

    // Mower move is not readable
    case MoveNotValidEvent =>
      log.warning(s"move received is not readable, please make sure moves contain only characters from {L,R,F}")
      currentMower ! MowerActor.MoveNotValid
      become(readyForMoves(grid))
      self ! NextMove(remainingMowers :+ currentMower)

    // The current Mower has no more move
    case NoMoreMoveEvent =>
      become(readyForMoves(grid))
      self ! NextMove(remainingMowers)
  }

  /**
   * Finally, in this state, we loop through all the mowers and ask for their final position.
   * We use the ask pattern here. Therefore, these calls are futures.
   */
  def readyToCollectFinalPosition(finishedMowers: Queue[ActorRef]): Receive = {
    case CollectFinalPosition =>
      finishedMowers foreach {
        case mowerActor =>
          val finalPositionFuture = (mowerActor ? MowerActor.FinalPosition).mapTo[MowerActor.FinalPosition]
          finalPositionFuture onComplete {
            case Success(FinalPosition(finalX, finalY, finalO)) =>
              log.info(s"$finalX $finalY $finalO")
          }
      }
  }
}
