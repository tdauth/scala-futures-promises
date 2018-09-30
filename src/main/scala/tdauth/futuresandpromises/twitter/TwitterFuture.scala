package tdauth.futuresandpromises.twitter

import com.twitter.util.Await

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class TwitterFuture[T](val future: com.twitter.util.Future[T], var ex: Executor) extends Future[T] {
  def get: T = Await.result(future)

  def isReady: Boolean = Await.isReady(future)

  override def then[S](f: (Try[T]) => S): Future[S] = {
    // TODO #18 Simplify the implementation by using something like transform from Scala FP which does not have to return a future.
    val transformCallback = (t: com.twitter.util.Try[T]) => TwitterUtil.async(ex, () => f.apply(new TwitterTry[T](t))).asInstanceOf[TwitterFuture[S]].future
    val transformFuture = future.transform[S](transformCallback)
    new TwitterFuture[S](transformFuture, ex)
  }

  override def factory: Factory = new TwitterFactory

}