package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class NonDerivedFactory extends Factory {
  override def createPromise[T](ex : Executor): Promise[T] = new NonDerivedPromise[T](ex)
  override def createTry() : Try[Unit] =  new NonDerivedTry[Unit](scala.util.Success())
  override def createTryFromValue[T](v: T): Try[T] = new NonDerivedTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new NonDerivedTry[T](scala.util.Failure(e))
}