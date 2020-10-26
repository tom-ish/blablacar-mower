package model

sealed trait Command
case object L extends Command
case object R extends Command
case object F extends Command

