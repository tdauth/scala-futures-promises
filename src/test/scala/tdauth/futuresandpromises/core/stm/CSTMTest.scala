package tdauth.futuresandpromises.core.stm

import java.util.concurrent.Executors

import tdauth.futuresandpromises.core.FP
import tdauth.futuresandpromises.{AbstractFPTest, JavaExecutor}

class CSTMTest extends AbstractFPTest {
  override def getTestName: String = "CSTMTest"
  override def getFP: FP[Int] = new CSTM[Int](executor)

  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())
}
