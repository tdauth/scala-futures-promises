package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Util

class ScalaFPUtil extends Util {
  override def async[T](ex: Executor, f: () => T): Future[T] = {
    val executionContext: scala.concurrent.ExecutionContext = ex.asInstanceOf[ScalaFPExecutor].executionContext
    val future: scala.concurrent.Future[T] = scala.concurrent.Future {
      f()
    }(executionContext)

    new ScalaFPFuture[T](future)
  }

  override def factory = new ScalaFPFactory
}

object ScalaFPUtil {
  private val util: ScalaFPUtil = new ScalaFPUtil

  def async[T](ex: Executor, f: () => T): Future[T] = {
    util.async[T](ex, f)
  }

  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = {
    util.firstN[T](c, n)
  }

  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNSuccResultType[T]] = {
    util.firstNSucc[T](c, n)
  }
}