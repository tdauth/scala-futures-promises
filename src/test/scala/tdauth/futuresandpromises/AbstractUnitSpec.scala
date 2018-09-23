package tdauth.futuresandpromises

import org.scalatest.FlatSpec
import org.scalatest.Matchers
import scala.concurrent.blocking

abstract class AbstractUnitSpec extends FlatSpec with Matchers {
  def delay() = blocking { Thread.sleep(4000) }

  def getTestName: String
}