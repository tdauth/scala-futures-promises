package futuresandpromises.standardlibrary

import org.scalatest._
import main.scala.futuresandpromises.standardlibrary.ScalaFPTry
import main.scala.futuresandpromises.UsingUninitializedTry

class TryTest extends FlatSpec with Matchers {
  "An empty try" should "throw the exception UsingUninitializedTry" in {
    val t = new ScalaFPTry[Int]()
    the[UsingUninitializedTry] thrownBy t.get should have message null
  }

  "A successful try" should "contain the value 1" in {
    val t = new ScalaFPTry[Int](scala.util.Success(1))
    t.get should be(1)
  }

  "A failed try" should "throw an exception" in {
    val t = new ScalaFPTry[Int](scala.util.Failure(new RuntimeException("Error")))
    the[RuntimeException] thrownBy t.get should have message "Error"
  }
}