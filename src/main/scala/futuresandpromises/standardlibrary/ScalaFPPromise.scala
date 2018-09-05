package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Promise
import main.scala.futuresandpromises.Future
import main.scala.futuresandpromises.Try

class ScalaFPPromise[T]() extends Promise[T] {
  private val p = scala.concurrent.Promise.apply[T]

  override def future(): Future[T] = new ScalaFPFuture(p.future)
  override def tryComplete(v: Try[T]): Boolean = p.tryComplete(v.asInstanceOf[ScalaFPTry[T]].t)
  override def factory = new ScalaFPFactory
}