package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractTryTest
import tdauth.futuresandpromises.Try

class TryTest extends AbstractTryTest {
  def getTry[T]: Try[T] = new ScalaFPTry[T]
  def getTrySucc[T](v: T): Try[T] = new ScalaFPTry[T](scala.util.Success(v))
  def getTryFailure[T](e: Throwable): Try[T] = new ScalaFPTry[T](scala.util.Failure(e))
}