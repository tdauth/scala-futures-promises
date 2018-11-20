package tdauth.futuresandpromises.core.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.core.FP
import tdauth.futuresandpromises.{AbstractFPTest, JavaExecutor}

class CCASOneCallbackAtATimeTest extends AbstractFPTest {
  override def getTestName: String = "CCASOneCallbackAtATimeTest"
  override def getFP: FP[Int] = new CCASOneCallbackAtATime[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}
