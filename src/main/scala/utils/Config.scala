package utils

import com.typesafe.config.ConfigFactory

trait Config {
  val appConfig = ConfigFactory.load.getConfig("settings")
}
