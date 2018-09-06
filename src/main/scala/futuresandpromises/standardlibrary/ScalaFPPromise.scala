package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Promise
import main.scala.futuresandpromises.Future
import main.scala.futuresandpromises.Try
import main.scala.futuresandpromises.UsingUninitializedTry

class ScalaFPPromise[T]() extends Promise[T] {
  private val p = scala.concurrent.Promise.apply[T]

  override def future(): Future[T] = new ScalaFPFuture(p.future)

  override def tryComplete(v: Try[T]): Boolean = {
    val o = v.asInstanceOf[ScalaFPTry[T]].o
    o match {
      case Some(t) => p.tryComplete(t)
      case None => throw new UsingUninitializedTry
    }
  }

  override def factory = new ScalaFPFactory
}