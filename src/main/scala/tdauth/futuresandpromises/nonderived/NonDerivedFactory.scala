package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future

class NonDerivedFactory extends Factory {
  override def createPromise[T] = new NonDerivedPromise[T]
  override def createTryFromValue[T](v: T) = new NonDerivedTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable) = new NonDerivedTry[T](scala.util.Failure(e))
  override def assignExecutorToFuture[T](f: Future[T], e: Executor) = f.asInstanceOf[NonDerivedFuture[T]].ex = e.asInstanceOf[NonDerivedExecutor]
}