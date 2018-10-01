package tdauth.futuresandpromises.twitter

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Util

class TwitterUtil extends Util {
  override def factory: Factory = new TwitterFactory
}

object TwitterUtil {
  private val util = new TwitterUtil

  def async[T](ex: Executor, f: () => T): Future[T] = util.async[T](ex, f)
  def firstN[T](ex : Executor, c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = util.firstN[T](ex, c, n)
  def firstNSucc[T](ex : Executor, c: Vector[Future[T]], n: Integer): Future[Util#FirstNSuccResultType[T]] = util.firstNSucc[T](ex, c, n)
}