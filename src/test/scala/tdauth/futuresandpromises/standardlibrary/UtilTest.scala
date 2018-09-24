package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "ScalaFPUtil"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new ScalaFPUtil
  override def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}