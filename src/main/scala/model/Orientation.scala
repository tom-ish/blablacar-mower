package model

import scala.collection.immutable.HashMap

object Orientation {
  sealed abstract class Direction
  case object N extends Direction
  case object S extends Direction
  case object E extends Direction
  case object W extends Direction
  case object UnknownDirection extends Direction
  val orientations = HashMap('N' -> N, 'S' -> S, 'E' -> E, 'W' -> W)
}