package tdauth.futuresandpromises.mvar

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFPTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.FP

class PrimMVarTest extends AbstractFPTest {
  override def getTestName: String = "MVarFutureTest"
  override def getFP: FP[Int] = new PrimMVar[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}