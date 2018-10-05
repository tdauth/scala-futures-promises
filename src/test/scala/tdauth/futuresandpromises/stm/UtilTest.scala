package tdauth.futuresandpromises.stm

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "StmUtilTest"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new StmUtil
  override def getPromise: Promise[Int] = new StmPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())

  "StmUtil.async" should "complete a future successfully" in {
    val f = StmUtil.async(getExecutor, () => 10)

    f.get should be(10)
  }

  it should "fail a future" in {
    val f = StmUtil.async[Int](getExecutor, () => throw new RuntimeException("Failure!"))

    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }
}