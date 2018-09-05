package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Executor
import main.scala.futuresandpromises.Future
import main.scala.futuresandpromises.Util

class ScalaFPUtil extends Util {
  override def async[T](ex: Executor, f: () => T): Future[T] = {
    val executionContext : scala.concurrent.ExecutionContext = ex.asInstanceOf[ScalaFPExecutor].executionContext
    val future : scala.concurrent.Future[T] = scala.concurrent.Future {
      f()
    }(executionContext)

    new ScalaFPFuture[T](future)
  }
}

object ScalaFPUtil {
  private val util : ScalaFPUtil = new ScalaFPUtil

  override def async[T](ex: Executor, f: () => T): Future[T] = {
    util.async[T](ex, f)
  }

  // Derived methods:
  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Vector[Tuple2[Integer, Future[T]]]] = {
    util.firstN[T](c, n)
  }

  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Vector[Tuple2[Integer, T]]] = {
    util.firstNSucc[T](c, n)
  }

}