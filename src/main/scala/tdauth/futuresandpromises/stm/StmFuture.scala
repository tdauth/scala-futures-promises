package tdauth.futuresandpromises.stm

import scala.util.control.NonFatal

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class StmFuture[T](val s: StmSharedState[T]) extends Future[T] {

  override def get: T = s.get

  override def isReady: Boolean = s.isReady

  override def then[S](callback: Try[T] => S): Future[S] = {
    val p = factory.createPromise[S]
    val f = p.future()
    factory.assignExecutorToFuture(f, s.getExecutor)

    s.setCallback((t: Try[T]) => {
      try {
        val result = callback(t)
        p.trySuccess(result)
      } catch {
        case NonFatal(e) => p.tryFailure(e)
      }
    })

    f
  }

  def sync: Unit = s.sync

  override def factory = new StmFactory
}