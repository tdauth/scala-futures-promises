package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class StmPromise[T](s: StmSharedState[T]) extends Promise[T] {

  def this() = this(new StmSharedState[T])

  override def future(): Future[T] = new StmFuture[T](s)

  override def tryComplete(v: Try[T]): Boolean = s.tryComplete(v)

  override def factory = new StmFactory

}