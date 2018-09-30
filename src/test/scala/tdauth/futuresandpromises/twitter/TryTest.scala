package tdauth.futuresandpromises.twitter

import tdauth.futuresandpromises.AbstractTryTest
import tdauth.futuresandpromises.Try

class TryTest extends AbstractTryTest {
  override def getTestName: String = "TwitterTry"
  override def getTry[T]: Try[T] = new TwitterTry[T]
  override def getTrySucc[T](v: T): Try[T] = new TwitterTry[T](com.twitter.util.Return(v))
  override def getTryFailure[T](e: Throwable): Try[T] = new TwitterTry[T](com.twitter.util.Throw(e))
}