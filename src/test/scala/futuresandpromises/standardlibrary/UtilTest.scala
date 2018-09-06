package futuresandpromises.standardlibrary

import org.scalatest._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import main.scala.futuresandpromises.standardlibrary.ScalaFPExecutor
import main.scala.futuresandpromises.standardlibrary.ScalaFPUtil

class UtilTest extends FlatSpec with Matchers {
  "Exception" should "be thrown by firstN" in {
    val result = ScalaFPUtil.firstN(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  "Three futures" should "be returned from ten" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val futures = Vector.tabulate(10)(n => {
      ScalaFPUtil.async(executor, () => {
        Thread.sleep(1000)

        if (n % 2 == 0) {
          n
        } else {
          throw new Exception("test")
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

  "Exception" should "be thrown by firstNSucc" in {
    val result = ScalaFPUtil.firstNSucc(Vector(), 3)
    the[RuntimeException] thrownBy result.get should have message "Not enough futures"
  }

  "Three successful futures" should "be returned from ten" in {
    val executor = new ScalaFPExecutor(ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor()))
    val futures = Vector.tabulate(10)(n => {
      ScalaFPUtil.async(executor, () => {
        Thread.sleep(1000)

        if (n % 2 == 0) {
          n
        } else {
          throw new Exception("test")
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
}