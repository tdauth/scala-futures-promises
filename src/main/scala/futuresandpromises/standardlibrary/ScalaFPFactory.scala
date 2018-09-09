package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class ScalaFPFactory extends Factory {
  override def createPromise[T]: Promise[T] = new ScalaFPPromise[T]
  override def createTryFromValue[T](v: T): Try[T] = new ScalaFPTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Throwable): Try[T] = new ScalaFPTry[T](scala.util.Failure(e))
}