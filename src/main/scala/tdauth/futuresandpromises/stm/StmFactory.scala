package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class StmFactory extends Factory {
  override def createPromise[T]: Promise[T] = new StmPromise[T]
  override def createTryFromValue[T](v: T): Try[T] = new StmTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new StmTry[T](scala.util.Failure(e))
  override def assignExecutorToFuture[T](f: Future[T], e: Executor) = f.asInstanceOf[StmFuture[T]].s.setExecutor(e)
}