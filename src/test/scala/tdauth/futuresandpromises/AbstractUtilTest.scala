package tdauth.futuresandpromises

abstract class AbstractUtilTest extends AbstractUnitSpec {
  getTestName should "just print the name" in {
  }

  "async" should "complete a future successfully" in {
    val f = getUtil.async(getExecutor, () => 10)

    f.get should be(10)
  }

  it should "fail a future" in {
    val f = getUtil.async[Int](getExecutor, () => throw new RuntimeException("Failure!"))

    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }

  "firstN" should "throw an exception" in {
    val result = getUtil.firstN(getExecutor, Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val futures = produceFutures(3)
    val result = getUtil.firstN(getExecutor, futures, 3).get

    result.length should be(3)

    var counter = 0

    for (t <- result) {
      t._1 match {
        case 0 => {
          t._2.get should be(0)
          counter += 1
        }
        case 1 => {
          the[Exception] thrownBy t._2.get should have message "test"
          counter += 1
        }
        case 2 => {
          t._2.get should be(2)
          counter += 1
        }
        case _ => fail("Unexpected result: " + t)
      }
    }

    counter should be(3)
  }

  "firstNSucc" should "throw an exception" in {
    val result = getUtil.firstNSucc(getExecutor, Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three successful futures" in {
    val futures = produceFutures(5)
    val result = getUtil.firstNSucc(getExecutor, futures, 3).get

    result.length should be(3)

    var counter = 0

    for (t <- result) {
      t match {
        case (0, 0) => {
          counter += 1
        }
        case (2, 2) => {
          counter += 1
        }
        case (4, 4) => {
          counter += 1
        }
        case _ => fail("Unexpected result: " + t)
      }
    }

    counter should be(3)
  }

  it should "fail with one of the futures" in {
    val futures = produceFailedFutures(3)

    for ((f, i) <- futures.zipWithIndex) {
      the[RuntimeException] thrownBy f.get should have message "test " + i
    }

    val result = getUtil.firstNSucc(getExecutor, futures, 3)

    the[RuntimeException] thrownBy result.get should have message "test 0"
  }

  private def produceFutures(n: Int) = {
    Vector.tabulate(n)(n => {
      val p = getPromise
      if (n % 2 == 0) {
        p.trySuccess(n)
      } else {
        p.tryFailure(new RuntimeException("test"))
      }

      p.future()
    })
  }

  private def produceFailedFutures(n: Int) = {
    Vector.tabulate(n)(n => {
      val p = getPromise
      p.tryFailure(new RuntimeException("test " + n))
      p.future()
    })
  }

  def getExecutor: Executor
  def getUtil: Util
  def getPromise: Promise[Int]
}