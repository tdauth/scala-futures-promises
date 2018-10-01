package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class ScalaFPFactory extends Factory {
  override def createPromise[T](ex : Executor): Promise[T] = new ScalaFPPromise[T](ex)
  override def createTry(): Try[Unit] = new ScalaFPTry[Unit](scala.util.Success())
  override def createTryFromValue[T](v: T): Try[T] = new ScalaFPTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new ScalaFPTry[T](scala.util.Failure(e))
}