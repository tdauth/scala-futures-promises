package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractFPTest, FP, JavaExecutor}

class PrimCASOneCallbackAtATimeTest extends AbstractFPTest {
  override def getTestName: String = "PrimCASOneCallbackAtATimeTest"
  override def getFP: FP[Int] = new PrimCASOneCallbackAtATime[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}