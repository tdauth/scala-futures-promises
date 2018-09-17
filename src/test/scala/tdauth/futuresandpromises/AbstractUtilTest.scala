package tdauth.futuresandpromises

abstract class AbstractUtilTest extends AbstractUnitSpec {
  "firstN" should "should throw an exception" in {
    val result = getUtil.firstN(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val executor = getExecutor
    val futures = Vector.tabulate(3)(n => {
      getUtil.async(executor, () => {
        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = getUtil.firstN(futures, 3).get

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
    val result = getUtil.firstNSucc(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three successful futures" in {
    val executor = getExecutor
    val futures = Vector.tabulate(5)(n => {
      getUtil.async(executor, () => {
        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = getUtil.firstNSucc(futures, 3).get

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
    val executor = getExecutor
    val futures = Vector.tabulate(5)(n => {
      val f = getUtil.async[Int](executor, () => {
        throw new RuntimeException("test " + n)
      })
      f.sync
      f
    })

    val result = getUtil.firstNSucc(futures, 3)

    the[RuntimeException] thrownBy result.get should have message "test 2"
  }

  def getExecutor: Executor
  def getUtil: Util
}