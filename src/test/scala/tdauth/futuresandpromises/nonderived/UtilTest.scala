package tdauth.futuresandpromises.nonderived

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractUtilTest, Executor, Promise, Util}

import scala.concurrent.ExecutionContext

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "NonDerivedUtil"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new NonDerivedUtil
  override def getPromise: Promise[Int] = new NonDerivedPromise[Int](executor)

  private val executor = new NonDerivedExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}
