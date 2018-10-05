package tdauth.futuresandpromises.twitter

import scala.util.control.NonFatal

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.UsingUninitializedTry

// TODO #18 Use Twitter's FuturePool instead?
class TwitterPromise[T](executor: Executor) extends Promise[T] {
  protected val promise = com.twitter.util.Promise.apply[T]

  override def future(): Future[T] = new TwitterFuture[T](promise, executor)

  override def tryComplete(v: Try[T]): Boolean = {
    if (v.hasValue || v.hasException) {
      try {
        val r = v.get
        promise.updateIfEmpty(com.twitter.util.Return(r))
      } catch {
        case NonFatal(x) => promise.updateIfEmpty(com.twitter.util.Throw(x))
      }
    } else {
      promise.updateIfEmpty(com.twitter.util.Throw(new UsingUninitializedTry))
    }
  }
}