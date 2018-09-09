package tdauth.futuresandpromises.combinators

import tdauth.futuresandpromises.UnitSpec
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor

class UtilTest extends UnitSpec {
  "firstNWithFirst" should "should throw an exception" in {
    val result = CombinatorsUtil.firstNWithFirst(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val executor = new ScalaFPExecutor
    val futures = Vector.tabulate(3)(n => {
      CombinatorsUtil.async(executor, () => {
        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = CombinatorsUtil.firstNWithFirst(futures, 3).get

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
}