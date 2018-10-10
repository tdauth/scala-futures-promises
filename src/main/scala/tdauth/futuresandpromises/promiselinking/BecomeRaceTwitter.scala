package tdauth.futuresandpromises.promiselinking

import java.util.concurrent.atomic.AtomicInteger

import com.twitter.util.Await
import com.twitter.util.Promise
import com.twitter.util.Return

/**
 * When `become` is called two times, all callbacks are moved into `p`.
 * This means the counter `s` becomes 3.
 * It does not matter if `p` is completed by the completion of `p1` or `p2`.
 * This is invalid behavior since `p0` is now equal to either `p1` OR `p2` but has called the callbacks of both promises.
 * This behavior only occurs because the callbacks are moved to the left promise.
 */
object BecomeRaceTwitter extends App {
  val counter = new AtomicInteger(0)
  val p0 = Promise.apply[Int]
  val p1 = Promise.apply[Int]
  val p2 = Promise.apply[Int]

  def callback(msg: String) = {
    counter.incrementAndGet()
    println(msg)
  }

  p0.respond(_ => callback("Respond p0"))
  p1.respond(_ => callback("Respond p1"))
  p2.respond(_ => callback("Respond p2"))

  // Consider that become cannot be used on a completed promises.
  p0.become(p1)
  p0.become(p2)

  // Now start a race where only one will succeed!
  //p1.updateIfEmpty(Return(1))
  p2.updateIfEmpty(Return(2))

  val result = Await.result(p0)
  println("Result: " + result + " with a counter of " + counter.get)
  assert(counter.get == 3)
}