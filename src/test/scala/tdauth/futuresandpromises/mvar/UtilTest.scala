package tdauth.futuresandpromises.mvar

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "MVarUtilTest"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new MVarUtil
  override def getPromise: Promise[Int] = new MVarPromise[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())

  "MVarUtil.async" should "complete a future successfully" in {
    val f = MVarUtil.async(getExecutor, () => 10)

    f.get should be(10)
  }

  it should "fail a future" in {
    val f = MVarUtil.async[Int](getExecutor, () => throw new RuntimeException("Failure!"))

    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }
}