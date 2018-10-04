package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractUtilTest
import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Util

class UtilTest extends AbstractUtilTest {
  override def getTestName: String = "CasUtil"
  override def getExecutor: Executor = executor
  override def getUtil: Util = new CasUtil
  override def getPromise: Promise[Int] = new CasPromise[Int](executor)

  private val executor = new CasExecutor(Executors.newSingleThreadExecutor())

  "CasUtil.async" should "complete a future successfully" in {
    val f = CasUtil.async(getExecutor, () => 10)

    f.get should be(10)
  }

  it should "fail a future" in {
    val f = CasUtil.async[Int](getExecutor, () => throw new RuntimeException("Failure!"))

    the[RuntimeException] thrownBy f.get should have message "Failure!"
  }
}