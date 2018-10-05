package tdauth.futuresandpromises.cas

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory

class CasFuture[T](s: CasSharedState[T], ex: Executor) extends Future[T] {

  override def get: T = s.getResult

  override def isReady: Boolean = s.isReady

  override def onComplete(f: (Try[T]) => Unit): Unit = s.onComplete(f)

  override def getExecutor: Executor = ex

  override def factory: Factory = new CasFactory
}