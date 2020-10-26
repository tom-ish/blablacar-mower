package utils

import actors.MowerActor
import akka.actor.{ActorRef, ActorSystem, Props}
import model.Orientation.{UnknownDirection, orientations}

import scala.annotation.tailrec
import scala.collection.immutable.Queue

object InitHelper extends Config {
  def readLines(lines: Iterator[String], width: Int, height: Int)
               (implicit actorSystem: ActorSystem): Queue[ActorRef] = {
    @tailrec
    def updateMowersQueue(lines: Iterator[String],
                          initialPositionOption: Option[String],
                          accumulator: Queue[ActorRef]): Queue[ActorRef] = {
      lines.nextOption() match {
        /**
         * if splitting the line results in an array of size 3, we assume the line contains a new mower's initial position
         **/
        case Some(line) if line.split(" ").length == 3 =>
          updateMowersQueue(lines, Some(line), accumulator)
        /**
         * if the line does not contain a space, we assume the line represents moves sequence:
         * the recursive call should have set a new mower's initial position if this line is read
         * otherwise, we consider the moves sequence is not mapped to any mower
         **/
        case Some(line) if !line.contains(" ") =>
          initialPositionOption match {
            case Some(initialPosition) =>
              val newMowerActor = actorSystem.actorOf(InitHelper.initMowerProps(
                positionInfo = initialPosition,
                moves = line,
                gridWidth = width,
                gridHeight = height
              ))
              updateMowersQueue(lines, None, accumulator :+ newMowerActor)
            case None =>
              accumulator
          }
        /**
         * if any of the two cases above is matched (no new line to read or invalid line), we stop the recursion
         **/
        case _ =>
          accumulator
      }
    }
    updateMowersQueue(lines, None, Queue.empty)
  }

  def getGridInfo(gridInfoLine: String): (Integer, Integer) = {
    val gridInfo: Array[String] = gridInfoLine.split(" ")
    val topRightCornerX = gridInfo(0)
    val topRightCornerY = gridInfo(1)
    (Integer.valueOf(topRightCornerX), Integer.valueOf(topRightCornerY))
  }

  def initMowerProps(positionInfo: String, moves: String,  gridWidth: Int, gridHeight: Int): Props = {
    val initialPosition = positionInfo.split(" ")
    val initialX = initialPosition(0)
    val initialY = initialPosition(1)
    val initialO = initialPosition(2).charAt(0)
    val initialOrientation = orientations.getOrElse(
      initialO,
      UnknownDirection
    )

    MowerActor.props(
      Integer.parseInt(initialX),
      Integer.parseInt(initialY),
      initialOrientation,
      moves,
      gridWidth,
      gridHeight
    )
  }
}
