package tdauth.futuresandpromises.core.mvar

import java.util.concurrent.Executors

import tdauth.futuresandpromises.core.FP
import tdauth.futuresandpromises.{AbstractFPTest, JavaExecutor}

class CMVarTest extends AbstractFPTest {
  override def getTestName: String = "CMVarTest"
  override def getFP: FP[Int] = new CMVar[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}
