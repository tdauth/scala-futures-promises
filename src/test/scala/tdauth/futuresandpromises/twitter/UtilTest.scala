package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "TwitterUtil"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new TwitterUtil
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)

  private val executor = new TwitterExecutor(Executors.newSingleThreadExecutor())

  "TwitterUtil.async" should "complete a future successfully" in {
    val f = TwitterUtil.async(getExecutor, () => 10)

    f.get should be(10)
  }

  it should "fail a future" in {
    val f = TwitterUtil.async[Int](getExecutor, () => throw new RuntimeException("Failure!"))

    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }
}