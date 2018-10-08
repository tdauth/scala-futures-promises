package tdauth.futuresandpromises.stm

import java.util.concurrent.Executors

import tdauth.futuresandpromises.AbstractFPTest
import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.FP

class PrimSTMTest extends AbstractFPTest {
  override def getTestName: String = "StmFutureTest"
  override def getFP: FP[Int] = new PrimSTM[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}