package tdauth.futuresandpromises

abstract class AbstractTryTest extends AbstractUnitSpec {
  getTestName should "throw the exception UsingUninitializedTry" in {
    val t = getTry[Int]
    t.hasException should be(false)
    t.hasValue should be(false)
    the[UsingUninitializedTry] thrownBy t.get should have message null
  }

  it should "contain the value 1" in {
    val t = getTrySucc[Int](1)
    t.hasException should be(false)
    t.hasValue should be(true)
    t.get should be(1)
  }

  it should "throw an exception" in {
    val t = getTryFailure[Int](new RuntimeException("Error"))
    t.hasException should be(true)
    t.hasValue should be(false)
    the[RuntimeException] thrownBy t.get should have message "Error"
  }

  def getTry[T]: Try[T]
  def getTrySucc[T](v: T): Try[T]
  def getTryFailure[T](e: Throwable): Try[T]
}