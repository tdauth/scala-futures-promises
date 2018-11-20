package tdauth.futuresandpromises.promiselinking

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.SyncVar

import com.twitter.util.Await
import com.twitter.util.Promise
import com.twitter.util.Return
import com.twitter.util.Try

/**
 * When `become` is called two times, all callbacks are moved into `p`.
 * This means the counter `s` becomes 3.
 * It does not matter if `p` is completed by the completion of `p1` or `p2`.
 * This is invalid behavior since `p0` is now equal to either `p1` OR `p2` but has called the callbacks of both promises.
 * This behavior only occurs because the callbacks are moved to the left promise.
 */
object BecomeRaceTwitter extends App {
  val counter = new AtomicInteger(0)
  val s = new SyncVar[Unit]
  val p0 = Promise[Int]
  val p1 = Promise[Int]
  val p2 = Promise[Int]

  def callback(msg: String, x: Try[Int]): Unit = {
    val v = counter.incrementAndGet()
    println("%s: completes with value %d".format(msg, x.get))
    if (v == 3) s.put(())
  }

  p0.respond(x => callback("Respond p0", x))
  p1.respond(x => callback("Respond p1", x))
  p2.respond(x => callback("Respond p2", x))

  // Consider that become cannot be used on a completed promises.
  p0.become(p1)
  p0.become(p2)

  // Now start a race where only one will succeed!
  //p1.updateIfEmpty(Return(1))
  p2.updateIfEmpty(Return(2))

  s.get
  val result = Await.result(p0)
  println("Result: " + result + " with a counter of " + counter.get)
  assert(counter.get == 3)
}