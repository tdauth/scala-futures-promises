package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import com.twitter.util.FuturePool
import tdauth.futuresandpromises.{AbstractUtilTest, Executor, Promise, Util}

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "TwitterUtil"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new TwitterUtil
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)

  private val executor = new TwitterExecutor(FuturePool(Executors.newSingleThreadExecutor()))

  "TwitterUtil.async" should "complete a future successfully" in {
    val f = TwitterUtil.async(getExecutor, () => 10)

    f.get should be(10)
  }

  it should "fail a future" in {
    val f = TwitterUtil.async[Int](getExecutor, () => throw new RuntimeException("Failure!"))

    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }
}
