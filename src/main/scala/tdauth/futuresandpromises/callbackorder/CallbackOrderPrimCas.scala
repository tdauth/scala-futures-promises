package tdauth.futuresandpromises.callbackorder

import java.util.concurrent.Executors

import scala.concurrent.SyncVar

import tdauth.futuresandpromises.JavaExecutor
import tdauth.futuresandpromises.cas.PrimCAS

object CallbackOrderPrimCas extends App {
  val ex = new JavaExecutor(Executors.newSingleThreadExecutor)
  val p = new PrimCAS[Int](ex)

  def callback(v: Int, s: SyncVar[List[Int]]) {
    val updated = s.take()
    s.put(updated :+ v)
  }

  val s = new SyncVar[List[Int]]
  s.put(List())

  1 to 10 foreach (i => p.onComplete(t => callback(i, s)))

  p.trySuccess(1)
  p.getP
  println("Callback execution order for PrimCAS: " + s.get)
  ex.shutdown()
}