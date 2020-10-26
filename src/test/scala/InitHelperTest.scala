import java.io.FileNotFoundException

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import utils.InitHelper

import scala.io.Source

class InitHelperTest extends TestKit(ActorSystem("test"))
  with AnyWordSpecLike
  with Matchers {
  "InitHelper" should  {
    "raise an error when unknown filename" in {
      // given
      val filename = "blabla.car"

      // when
      lazy val src = Source.fromFile(filename)

      // then
      assertThrows[FileNotFoundException](src)
    }

    "retrieve top right corner position" in {
      // given
      val filename = "input.txt"
      val src = Source.fromFile(filename)
      val lines = src.getLines()
      val gridInfoLine = lines.next()

      // when
      val (topRightCornerX, topRightCornerY) = InitHelper.getGridInfo(gridInfoLine)

      // then
      assert(topRightCornerX == 5)
      assert(topRightCornerY == 5)
    }

    "initialize two mowers in the queue" in {
      // given
      val filename = "input.txt"
      val src = Source.fromFile(filename)
      val lines = src.getLines()
      val gridInfoLine = lines.next()
      val (topRightCornerX, topRightCornerY) = InitHelper.getGridInfo(gridInfoLine)
      val width = topRightCornerX+1
      val height = topRightCornerY+1

      // when
      val mowers = InitHelper.readLines(lines, width, height)

      // then
      assert(mowers.length == 2)
    }
  }
}
