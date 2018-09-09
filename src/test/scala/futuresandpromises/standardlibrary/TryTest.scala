package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.UsingUninitializedTry

class TryTest extends UnitSpec {
  "A try" should "throw the exception UsingUninitializedTry" in {
    val t = new ScalaFPTry[Int]()
    t.hasException should be(false)
    t.hasValue should be(false)
    the[UsingUninitializedTry] thrownBy t.get should have message null
  }

  it should "contain the value 1" in {
    val t = new ScalaFPTry[Int](scala.util.Success(1))
    t.hasException should be(false)
    t.hasValue should be(true)
    t.get should be(1)
  }

  it should "throw an exception" in {
    val t = new ScalaFPTry[Int](scala.util.Failure(new RuntimeException("Error")))
    t.hasException should be(true)
    t.hasValue should be(false)
    the[RuntimeException] thrownBy t.get should have message "Error"
  }
}