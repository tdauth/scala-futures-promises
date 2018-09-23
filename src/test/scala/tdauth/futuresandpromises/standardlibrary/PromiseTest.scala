package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Util

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "ScalaFPPromise"
  override def getExecutor: Executor = new ScalaFPExecutor
  override def getUtil: Util = new ScalaFPUtil
  override def getPromise[T]: Promise[T] = new ScalaFPPromise[T]
  override def getTry[T]: Try[T] = new ScalaFPTry[T]
}