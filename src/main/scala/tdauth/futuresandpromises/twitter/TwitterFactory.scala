package tdauth.futuresandpromises.twitter

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class TwitterFactory extends Factory {
  override def createPromise[T](ex : Executor): Promise[T] = new TwitterPromise[T](ex)
  override def createTry(): Try[Unit] = new TwitterTry[Unit](com.twitter.util.Return())
  override def createTryFromValue[T](v: T): Try[T] = new TwitterTry[T](com.twitter.util.Return(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new TwitterTry[T](com.twitter.util.Throw(e))
}