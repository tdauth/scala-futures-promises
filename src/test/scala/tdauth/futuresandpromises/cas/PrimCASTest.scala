package tdauth.futuresandpromises.cas

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFPTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.FP

class PrimCASTest extends AbstractFPTest {
  override def getTestName: String = "CasFutureTest"
  override def getFP: FP[Int] = new PrimCAS[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}