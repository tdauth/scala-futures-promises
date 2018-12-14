package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import com.twitter.util.FuturePool
import tdauth.futuresandpromises.{AbstractFutureTest, Promise}

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "TwitterFuture"
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)

  private val executor = new TwitterExecutor(FuturePool(Executors.newSingleThreadExecutor()))
}
