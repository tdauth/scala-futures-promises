package tdauth.futuresandpromises.standardlibrary

import scala.util.control.NonFatal

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

/**
 * @param executor This executor is passed on to created futures from the promise.
 */
class ScalaFPPromise[T](executor: Executor = ScalaFPExecutor.global) extends Promise[T] {
  protected val promise = scala.concurrent.Promise.apply[T]

  override def future(): Future[T] = new ScalaFPFuture(promise.future, executor)

  override def tryComplete(v: Try[T]): Boolean = {
    try {
      promise.trySuccess(v.get())
    } catch {
      case NonFatal(e) => promise.tryFailure(e)
    }
  }
}