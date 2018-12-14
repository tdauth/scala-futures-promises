package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import com.twitter.util.FuturePool
import tdauth.futuresandpromises.{AbstractPromiseTest, Promise}

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "TwitterPromise"
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)

  private val executor = new TwitterExecutor(FuturePool(Executors.newSingleThreadExecutor()))
}
