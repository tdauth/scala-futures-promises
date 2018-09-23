package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.AbstractTryTest
import tdauth.futuresandpromises.Try

class TryTest extends AbstractTryTest {
  override def getTestName: String = "NonDerivedTry"
  override def getTry[T]: Try[T] = new NonDerivedTry[T]
  override def getTrySucc[T](v: T): Try[T] = new NonDerivedTry[T](scala.util.Success(v))
  override def getTryFailure[T](e: Throwable): Try[T] = new NonDerivedTry[T](scala.util.Failure(e))
}