package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class StmFuture[T](s: StmSharedState[T], ex: Executor) extends Future[T] {
  override def get: T = s.getResult

  override def isReady: Boolean = s.isReady

  override def onComplete(f: (Try[T]) => Unit): Unit = s.onComplete(f)

  override def getExecutor: Executor = ex

  override def factory: Factory = new StmFactory
}