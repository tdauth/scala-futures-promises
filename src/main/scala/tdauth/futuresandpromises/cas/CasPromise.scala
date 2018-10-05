package tdauth.futuresandpromises.cas

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

/**
 * @param executor This executor is passed on to created futures from the promise.
 */
class CasPromise[T](ex: Executor = JavaExecutor.global) extends Promise[T] {

  val s = new CasSharedState[T](ex)

  override def future(): Future[T] = new CasFuture(s, ex)

  override def tryComplete(v: Try[T]): Boolean = s.tryComplete(v)

  override def factory: Factory = new CasFactory
}