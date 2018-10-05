package tdauth.futuresandpromises.cas

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

/**
 * @param executor This executor is passed on to created futures from the promise.
 */
class CasPromise[T](ex: Executor) extends Promise[T] {

  val s = new CasSharedState[T](ex)

  override def future(): Future[T] = new CasFuture(s)

  override def tryComplete(v: Try[T]): Boolean = s.tryComplete(v)
}