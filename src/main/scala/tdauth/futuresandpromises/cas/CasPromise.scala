package tdauth.futuresandpromises.cas

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

/**
 * @param executor This executor is passed on to created futures from the promise.
 */
class CasPromise[T](ex: Executor = CasExecutor.global) extends Promise[T] with Future[T] {

  val s = new CasSharedState[T](ex)

  override def get: T = s.getResult

  override def isReady: Boolean = s.isReady

  override def onComplete(f: (Try[T]) => Unit): Unit = s.onComplete(f)

  override def getExecutor: Executor = ex

  override def future(): Future[T] = this

  override def tryComplete(v: Try[T]): Boolean = s.tryComplete(v)

  override def factory: Factory = new CasFactory
}