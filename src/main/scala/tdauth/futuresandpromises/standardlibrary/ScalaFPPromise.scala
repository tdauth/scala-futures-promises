package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.UsingUninitializedTry

class ScalaFPPromise[T] extends Promise[T] {
  protected val p = scala.concurrent.Promise.apply[T]

  override def future(): Future[T] = new ScalaFPFuture(p.future, ScalaFPExecutor.global)

  override def tryComplete(v: Try[T]): Boolean = {
    val o = v.asInstanceOf[ScalaFPTry[T]].o
    o match {
      case Some(t) => p.tryComplete(t)
      case None => p.tryFailure(new UsingUninitializedTry)
    }
  }

  override def factory: Factory = new ScalaFPFactory
}