package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class ScalaFPFactory extends Factory {
  override def createPromise[T]: Promise[T] = new ScalaFPPromise[T]
  override def createTry() : Try[Unit] =  new ScalaFPTry[Unit](scala.util.Success())
  override def createTryFromValue[T](v: T): Try[T] = new ScalaFPTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new ScalaFPTry[T](scala.util.Failure(e))
  override def assignExecutorToFuture[T](f: Future[T], e: Executor): Unit = f.asInstanceOf[ScalaFPFuture[T]].ex = e.asInstanceOf[ScalaFPExecutor]
}