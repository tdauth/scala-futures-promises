package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class StmPromise[T](ex: Executor) extends Promise[T] {
  val s = new StmSharedState[T](ex)

  override def future(): Future[T] = new StmFuture(s)

  override def tryComplete(v: Try[T]): Boolean = s.tryComplete(v)

  override def factory: Factory = new StmFactory
}