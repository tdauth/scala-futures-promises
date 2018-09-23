package tdauth.futuresandpromises

import org.scalatest.FlatSpec
import org.scalatest.Matchers

abstract class AbstractUnitSpec extends FlatSpec with Matchers {
  def delay() = {
    Thread.sleep(3000);
  }

  def getTestName: String
}