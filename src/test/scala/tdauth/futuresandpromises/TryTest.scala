package tdauth.futuresandpromises

class TryTest extends AbstractUnitSpec {
  override def getTestName : String = "TryTest"

  getTestName should "contain the value 1" in {
    val t = new Try[Int](1)
    t.hasException should be(false)
    t.hasValue should be(true)
    t.get should be(1)
  }

  it should "throw an exception" in {
    val t = new Try[Int](new RuntimeException("Error"))
    t.hasException should be(true)
    t.hasValue should be(false)
    the[RuntimeException] thrownBy t.get should have message "Error"
  }
}