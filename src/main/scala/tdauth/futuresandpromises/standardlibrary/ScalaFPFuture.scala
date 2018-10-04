package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.Await
import scala.concurrent.duration.Duration

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class ScalaFPFuture[T](future: scala.concurrent.Future[T], ex: Executor) extends Future[T] {

  override def get: T = Await.result(future, Duration.Inf)

  override def isReady: Boolean = future.isCompleted

  override def onComplete(f: (Try[T]) => Unit): Unit = future.onComplete(t => f.apply(new Try(t)))(ex.asInstanceOf[ScalaFPExecutor].executionContext)

  override def getExecutor: Executor = ex

  override def factory: Factory = new ScalaFPFactory
}