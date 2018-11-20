package tdauth.futuresandpromises.a_memory
import java.util.concurrent.Executors

import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.core.stm.CSTM

class CSTMRecursiveMemoryTest extends AbstractRecursiveMemoryTest[CSTM[Int]] {
  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())

  def getTestName = "PrimSTMRecursiveMemoryTest"
  def successfulFuture(): CSTM[Int] = {
    val p = new CSTM[Int](executor)
    p.trySuccess(0)
    p
  }
  def flatMapOnSuccessfulFuture(i: Int, f: (Int) => CSTM[Int]): CSTM[Int] = {
    val p = new CSTM[Int](executor)
    p.trySuccess(i)
    p.followedByWith(f).asInstanceOf[CSTM[Int]]
  }

  def syncFuture(f: CSTM[Int]) = f.getP
}
