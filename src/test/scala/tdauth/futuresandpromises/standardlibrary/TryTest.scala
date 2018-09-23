package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractTryTest
import tdauth.futuresandpromises.Try

class TryTest extends AbstractTryTest {
  override def getTestName: String = "ScalaFPTry"
  override def getTry[T]: Try[T] = new ScalaFPTry[T]
  override def getTrySucc[T](v: T): Try[T] = new ScalaFPTry[T](scala.util.Success(v))
  override def getTryFailure[T](e: Throwable): Try[T] = new ScalaFPTry[T](scala.util.Failure(e))
}