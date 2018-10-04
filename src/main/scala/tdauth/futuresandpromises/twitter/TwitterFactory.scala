package tdauth.futuresandpromises.twitter

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class TwitterFactory extends Factory {
  override def createPromise[T](ex: Executor): Promise[T] = new TwitterPromise[T](ex)
}