package tdauth.futuresandpromises.stm

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.Util

class PromiseTest extends AbstractPromiseTest {
  def getExecutor: Executor = new StmExecutor
  def getUtil: Util = new StmUtil
  def getPromise[T]: Promise[T] = new StmPromise[T]
  def getTry[T]: Try[T] = new StmTry[T]
}