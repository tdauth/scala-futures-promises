package tdauth.futuresandpromises.memory

import com.twitter.util.Promise
import scala.concurrent.SyncVar
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.Duration
import com.twitter.util.Await
import com.twitter.util.Return

/**
 * When `become` is called two times, all callbacks are moved into `p`.
 * This means the result value is 3.
 * When it is completed by either one of them, all callbacks are called?
 */
object BecomeRace extends App {
  val s = new AtomicInteger(0)
  val p = Promise.apply[Int]
  val f0 = Promise.apply[Int]
  f0.respond(_ => s.incrementAndGet())
  val f1 = Promise.apply[Int]
  f1.respond(_ => s.incrementAndGet())
  p.become(f0)
  p.become(f1)
  val r = p.map(v => { s.incrementAndGet(); v })

  // now start a race!
  f1.updateIfEmpty(Return(2))
  f0.updateIfEmpty(Return(1))

  val result = Await.result(r)
  println("Result: " + result + " with a counter of " + s.get)
}