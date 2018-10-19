package tdauth.futuresandpromises.callbackorder

import java.util.concurrent.Executors

import scala.concurrent.SyncVar

import com.twitter.util.Await
import com.twitter.util.Promise

object CallbackOrderTwitterUtil extends App {
  val ex = Executors.newSingleThreadExecutor
  val fp = com.twitter.util.FuturePool(ex)
  val p = Promise[Int]

  def callback(v: Int, s: SyncVar[List[Int]]) {
    val updated = s.take()
    s.put(updated :+ v)
  }

  val s = new SyncVar[List[Int]]
  s.put(List())

  1 to 10 foreach (i => p.transform(t => fp(callback(i, s))))

  p.setValue(1)
  Await.ready(p)
  println("Callback execution order for Twitter Util: " + s.get)
  ex.shutdown
}