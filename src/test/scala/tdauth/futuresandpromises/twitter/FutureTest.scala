package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.JavaExecutor
import com.twitter.util.FuturePool

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "TwitterFuture"
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)

  private val executor = new TwitterExecutor(FuturePool(Executors.newSingleThreadExecutor()))
}