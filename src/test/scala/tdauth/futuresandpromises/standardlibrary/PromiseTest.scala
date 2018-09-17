package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Util

class PromiseTest extends AbstractPromiseTest {
  def getExecutor: Executor = new ScalaFPExecutor
  def getUtil: Util = new ScalaFPUtil
  def getPromise[T]: Promise[T] = new ScalaFPPromise[T]
  def getTry[T]: Try[T] = new ScalaFPTry[T]
}