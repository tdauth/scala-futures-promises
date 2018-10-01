package tdauth.futuresandpromises.twitter

import com.twitter.util.Await

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class TwitterFuture[T](future: com.twitter.util.Future[T], ex: Executor) extends Future[T] {
  override def get: T = Await.result(future)

  override def isReady: Boolean = Await.isReady(future)

  override def onComplete(f: (Try[T]) => Unit): Unit = future.respond(t => f.apply(new TwitterTry[T](t)))

  override def getExecutor: Executor = ex

  override def factory: Factory = new TwitterFactory
}