package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Util
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

class NonDerivedUtil extends ScalaFPUtil {
  // Basic methods:
  override def factory = new NonDerivedFactory

  // Derived methods:
  override def async[T](ex: Executor, f: () => T): Future[T] = {
    val executor = ex.asInstanceOf[NonDerivedExecutor]
    new NonDerivedFuture[T](scala.concurrent.Future { f() }(executor.executionContext), executor)
  }

  /**
   * @groupdesc firstNCombinators Has to be implemented manually since there is no such method in Scala FP: https://stackoverflow.com/questions/52408674/do-scala-futures-support-for-non-blocking-combinators-such-as-firstncompletedof
   *
   * @group firstNCombinators
   */
  override def firstN[T](futures: Vector[Future[T]], n: Integer): Future[FirstNResultType[T]] = super.firstN(futures, n)

  /**
   * @group firstNCombinators
   */
  override def firstNSucc[T](futures: Vector[Future[T]], n: Integer): Future[FirstNSuccResultType[T]] = super.firstNSucc(futures, n)
}

object NonDerivedUtil {
  private val util: NonDerivedUtil = new NonDerivedUtil

  def async[T](ex: Executor, f: () => T): Future[T] = util.async[T](ex, f)
  def firstN[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNResultType[T]] = util.firstN[T](c, n)
  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[Util#FirstNSuccResultType[T]] = util.firstNSucc[T](c, n)
}