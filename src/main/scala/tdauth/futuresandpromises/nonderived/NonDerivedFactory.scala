package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class NonDerivedFactory extends Factory {
  override def createPromise[T]: Promise[T] = new NonDerivedPromise[T]
  override def createTry() : Try[Unit] =  new NonDerivedTry[Unit](scala.util.Success())
  override def createTryFromValue[T](v: T): Try[T] = new NonDerivedTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new NonDerivedTry[T](scala.util.Failure(e))
  override def assignExecutorToFuture[T](f: Future[T], e: Executor): Unit = f.asInstanceOf[NonDerivedFuture[T]].ex = e.asInstanceOf[NonDerivedExecutor]
}