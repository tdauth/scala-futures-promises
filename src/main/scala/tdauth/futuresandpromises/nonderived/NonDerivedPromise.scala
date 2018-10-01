package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.Executor

class NonDerivedPromise[T](executor: Executor = NonDerivedExecutor.global) extends ScalaFPPromise[T](executor) {

  // Basic methods:
  override def future(): Future[T] = new NonDerivedFuture(promise.future, NonDerivedExecutor.global)

  override def factory: Factory = new NonDerivedFactory

  // Derived methods:
  override def trySuccess(v: T): Boolean = promise.trySuccess(v)

  override def tryFailure(e: Throwable): Boolean = promise.tryFailure(e)

  override def tryCompleteWith(f: Future[T]): Unit = promise.tryCompleteWith(f.asInstanceOf[NonDerivedFuture[T]].future)

  /**
   * Has to be implemented manually since there is no such method in Scala FP.
   */
  override def trySuccessWith(f: Future[T]): Unit = {
    val future = f.asInstanceOf[NonDerivedFuture[T]]

    future.future.onComplete((t: scala.util.Try[T]) => {
      t match {
        case scala.util.Success(v) => promise.trySuccess(v)
        case scala.util.Failure(e) => {}
      }
    })(future.getExecutor.asInstanceOf[ScalaFPExecutor].executionContext)
  }

  /**
   * Has to be implemented manually since there is no such method in Scala FP.
   */
  override def tryFailureWith(f: Future[T]): Unit = {
    val future = f.asInstanceOf[NonDerivedFuture[T]]

    future.future.onComplete((t: scala.util.Try[T]) => {
      t match {
        case scala.util.Success(v) => {}
        case scala.util.Failure(e) => promise.tryFailure(e)
      }
    })(future.getExecutor.asInstanceOf[ScalaFPExecutor].executionContext)
  }
}