package tdauth.futuresandpromises.cas

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class CasFuture[T](s: CasSharedState[T]) extends Future[T] {

  override def get: T = s.getP

  override def isReady: Boolean = s.isReady

  override def onComplete(f: (Try[T]) => Unit): Unit = s.onComplete(f)

  override def getExecutor: Executor = s.getEx

  override def factory: Factory = new CasFactory
}