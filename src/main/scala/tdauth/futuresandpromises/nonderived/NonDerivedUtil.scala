package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Util

class NonDerivedUtil extends Util {
  override def factory = new NonDerivedFactory
}

object NonDerivedUtil {
  private val util: NonDerivedUtil = new NonDerivedUtil

  def async[T](ex: Executor, f: () => T): Future[T] = util.async[T](ex, f)
  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = util.firstN[T](c, n)
  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNSuccResultType[T]] = util.firstNSucc[T](c, n)
}