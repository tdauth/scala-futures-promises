package tdauth.futuresandpromises.twitter

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "TwitterPromise"
  override def getPromise: Promise[Int] = new TwitterPromise[Int](executor)
  override def getTry: Try[Int] = new TwitterTry[Int]

  private val executor = new TwitterExecutor(Executors.newSingleThreadExecutor())
}