package tdauth.futuresandpromises.mvar

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

/**
 * @param executor This executor is passed on to created futures from the promise.
 */
class MVarPromise[T](ex: Executor) extends Promise[T] {

  private val s = new MVarSharedState[T](ex)

  override def future(): Future[T] = new MVarFuture(s)

  override def tryComplete(v: Try[T]): Boolean = s.tryComplete(v)
}