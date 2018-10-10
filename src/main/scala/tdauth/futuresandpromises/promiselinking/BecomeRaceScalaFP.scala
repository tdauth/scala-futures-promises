package tdauth.futuresandpromises.promiselinking

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.concurrent.duration.Duration

/**
 * The same as [[BecomeRaceTwitter]] but since Scala FP does not provide `become`, we have to call `flatMap` and
 * `tryCompleteWith` on the same promise with two different input promises.
 * However, this will produce only one link since `tryCompleteWith` does not use promise linking, so we have to complete the second promise
 * which was passed to `tryCompleteWith`.
 * TODO #22 How can we link two promises to the same on in Scala FP when each of the operations `flatMap`, `transformWith` and `recoverWith`
 * creates a new promise? We have no `become` method and rely on manual casting of the resulting future to a promise. Note that
 * promise linking is not used by `tryCompleteWith`.
 */
object BecomeRaceScalaFP extends App {
  implicit val ec = ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor())

  val counter = new AtomicInteger(0)
  val p1 = Promise.apply[Int]
  val p2 = Promise.apply[Int]

  def callback(msg: String) = {
    counter.incrementAndGet()
    println(msg)
  }

  //p0.future.onComplete(_ => callback("Respond p0"))
  p1.future.onComplete(_ => callback("Respond p1"))
  p2.future.onComplete(_ => callback("Respond p2"))

  /*
   * The same as:
   * p0.tryCompleteWith(p1)
   * p0.tryCompleteWith(p2)
   * but with promise linking only for the first one: p1 to p0.
   *
   * step 1: link from p1 to p0
   * step 2: no link from p2 to p0 since tryCompleteWith does not implement promise linking
   */
  val tmp = Promise.apply[Int]
  tmp.trySuccess(10)
  val p0 = tmp.future.flatMap(t => p1.future)
  p0.onComplete(_ => callback("Respond p0"))
  /*
   * This does not create a link but leads to calling the callbacks of p1 when p2 is completed which is wrong!
   * This "hack" is possible since we know that the standard implementation is DefaultPromise.
   */
  p0.asInstanceOf[Promise[Int]].tryCompleteWith(p2.future)

  // Now start a race! Does only work when completing p2, since there is only a link from p1 to p0.
  //p1.trySuccess(1)
  p2.trySuccess(2)

  val result = Await.result(p0, Duration.Inf)
  println("Result: " + result + " with a counter of " + counter.get)
  assert(counter.get == 3)
  // TODO #22 Is still blocking.
}