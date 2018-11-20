package tdauth.futuresandpromises.core.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.{AbstractFPTest, JavaExecutor}
import tdauth.futuresandpromises.core.FP

class CCASTest extends AbstractFPTest {
  override def getTestName: String = "CCASTest"
  override def getFP: FP[Int] = new CCAS[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}
