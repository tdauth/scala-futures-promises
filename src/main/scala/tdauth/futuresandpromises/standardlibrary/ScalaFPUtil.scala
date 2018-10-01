package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Util

class ScalaFPUtil extends Util {
  override def factory: Factory = new ScalaFPFactory
}

object ScalaFPUtil {
  private val util = new ScalaFPUtil

  def async[T](ex: Executor, f: () => T): Future[T] = util.async[T](ex, f)
  def firstN[T](ex: Executor, c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = util.firstN[T](ex, c, n)
  def firstNSucc[T](ex: Executor, c: Vector[Future[T]], n: Integer): Future[Util#FirstNSuccResultType[T]] = util.firstNSucc[T](ex, c, n)
}