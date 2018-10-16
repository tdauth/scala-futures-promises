package tdauth.futuresandpromises.a_memory

import tdauth.futuresandpromises.stm.PrimSTM
import tdauth.futuresandpromises.AbstractUnitSpec
import tdauth.futuresandpromises.stm.PrimSTM
import tdauth.futuresandpromises.JavaExecutor
import java.util.concurrent.Executors

class PrimSTMRecursiveMemoryTest extends AbstractRecursiveMemoryTest[PrimSTM[Int]] {
  private val executor = new JavaExecutor(Executors.newSingleThreadExecutor())

  def getTestName = "PrimSTMRecursiveMemoryTest"
  def successfulFuture(): PrimSTM[Int] = {
    val p = new PrimSTM[Int](executor)
    p.trySuccess(0)
    p
  }
  def flatMapOnSuccessfulFuture(i: Int, f: (Int) => PrimSTM[Int]): PrimSTM[Int] = {
    val p = new PrimSTM[Int](executor)
    p.trySuccess(i)
    p.followedByWith(f).asInstanceOf[PrimSTM[Int]]
  }

  def syncFuture(f: PrimSTM[Int]) = f.getP
}