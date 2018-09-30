package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFutureTest
import tdauth.futuresandpromises.Promise

class FutureTest extends AbstractFutureTest {
  override def getTestName: String = "TwitterFuture"
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)

  private val executor = new TwitterExecutor(Executors.newSingleThreadExecutor())
}