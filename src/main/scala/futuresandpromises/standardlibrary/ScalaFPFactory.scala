package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Factory
import main.scala.futuresandpromises.Promise
import main.scala.futuresandpromises.Try

class ScalaFPFactory extends Factory {

  override def createPromise[T]: Promise[T] = new ScalaFPPromise[T]
  override def createTryFromValue[T](v: T): Try[T] = new ScalaFPTry[T](scala.util.Success(v))
  override def createTryFromException[T](e: Exception): Try[T] = new ScalaFPTry[T](scala.util.Failure(e))
}