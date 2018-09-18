package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Util

class StmUtil extends Util {
  override def factory: Factory = new StmFactory
}

object StmUtil {
  private val util: StmUtil = new StmUtil

  def async[T](ex: Executor, f: () => T): Future[T] = util.async[T](ex, f)
  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = util.firstN[T](c, n)
  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNSuccResultType[T]] = util.firstNSucc[T](c, n)
}