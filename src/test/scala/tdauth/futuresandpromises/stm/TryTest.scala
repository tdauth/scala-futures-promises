package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.AbstractTryTest
import tdauth.futuresandpromises.Try

class TryTest extends AbstractTryTest {
  def getTry[T]: Try[T] = new StmTry[T]
  def getTrySucc[T](v: T): Try[T] = new StmTry[T](scala.util.Success(v))
  def getTryFailure[T](e: Throwable): Try[T] = new StmTry[T](scala.util.Failure(e))
}