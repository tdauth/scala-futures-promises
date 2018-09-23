package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Util

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "NonDerivedPromise"
  override def getExecutor: Executor = new NonDerivedExecutor
  override def getUtil: Util = new NonDerivedUtil
  override def getPromise[T]: Promise[T] = new NonDerivedPromise[T]
  override def getTry[T]: Try[T] = new NonDerivedTry[T]
}