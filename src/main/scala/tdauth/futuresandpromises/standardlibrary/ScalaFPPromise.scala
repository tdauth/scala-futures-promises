package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.UsingUninitializedTry
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Executor

/**
 * @param executor This executor is passed on to created futures from the promise.
 */
class ScalaFPPromise[T](executor: Executor = ScalaFPExecutor.global) extends Promise[T] {
  protected val promise = scala.concurrent.Promise.apply[T]

  override def future(): Future[T] = new ScalaFPFuture(promise.future, executor)

  override def tryComplete(v: Try[T]): Boolean = {
    val o = v.asInstanceOf[ScalaFPTry[T]].o
    o match {
      case Some(t) => promise.tryComplete(t)
      case None => promise.tryFailure(new UsingUninitializedTry)
    }
  }

  override def factory: Factory = new ScalaFPFactory
}