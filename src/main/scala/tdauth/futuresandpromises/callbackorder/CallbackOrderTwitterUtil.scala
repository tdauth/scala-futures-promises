package tdauth.futuresandpromises.callbackorder

import scala.concurrent.SyncVar

import com.twitter.util.Await
import com.twitter.util.Promise

object CallbackOrderTwitterUtil extends App {
  val p = Promise[Int]

  def callback(v: Int, s: SyncVar[List[Int]]) {
    val updated = s.take()
    s.put(updated :+ v)
  }

  val s = new SyncVar[List[Int]]
  s.put(List())

  // TODO Use an executor pls
  1 to 10 foreach (i => p.respond(t => callback(i, s)))

  p.setValue(1)
  Await.ready(p)
  println("Callback execution order for Twitter Util: " + s.get)
}