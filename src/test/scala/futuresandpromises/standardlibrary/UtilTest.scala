package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.UnitSpec

class UtilTest extends UnitSpec {
  "firstN" should "should throw an exception" in {
    val result = ScalaFPUtil.firstN(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three futures" in {
    val executor = new ScalaFPExecutor
    val futures = Vector.tabulate(5)(n => {
      ScalaFPUtil.async(executor, () => {
        Thread.sleep(n * 3000)

        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = ScalaFPUtil.firstN(futures, 3).get

    result.length should be(3)

    val t0 = result(0)
    t0._1 should be(0)
    t0._2.get should be(0)

    val t1 = result(1)
    t1._1 should be(1)
    the[Exception] thrownBy t1._2.get should have message "test"

    val t3 = result(2)
    t3._1 should be(2)
    t3._2.get should be(2)
  }

  "firstNSucc" should "throw an exception" in {
    val result = ScalaFPUtil.firstNSucc(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  it should "return three successful futures" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val futures = Vector.tabulate(5)(n => {
      ScalaFPUtil.async(executor, () => {
        Thread.sleep(n * 3000)

        if (n % 2 == 0) {
          n
        } else {
          throw new RuntimeException("test")
        }
      })
    })

    val result = ScalaFPUtil.firstNSucc(futures, 3).get

    result.length should be(3)

    val t0 = result(0)
    t0._1 should be(0)
    t0._2 should be(0)

    val t1 = result(1)
    t1._1 should be(2)
    t1._2 should be(2)

    val t3 = result(2)
    t3._1 should be(4)
    t3._2 should be(4)
  }

  it should "fail with one of the futures" in {
    val executor = new ScalaFPExecutor
    val futures = Vector.tabulate(5)(n => {
      ScalaFPUtil.async[Int](executor, () => {
        Thread.sleep(n * 3000)
        throw new RuntimeException("test " + n)
      })
    })

    val result = ScalaFPUtil.firstNSucc(futures, 3)

    the[RuntimeException] thrownBy result.get should have message "test 2"
  }
}