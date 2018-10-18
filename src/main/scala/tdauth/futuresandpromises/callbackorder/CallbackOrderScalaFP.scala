package tdauth.futuresandpromises.callbackorder

import java.util.concurrent.Executors

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Promise
import scala.concurrent.SyncVar
import scala.concurrent.duration.Duration

/**
 * Shows in which order the callbacks are executed when the promise is not completed yet.
 * `onComplete` makes no guarantees in Scala FP in which order the callbacks are executed.
 * Bear in mind that they are all concatenated whenever a callback is added.
 * Scala 2.12.xx used the :: operator. Scala 2.13.xx created a new instance of `ManyCallbacks` which is a link of the
 * existing callbacks (left) and the new callback (right)
 *
 * - Scala 2.12.xx: See method `dispatchOrAddCallback` in [[https://github.com/scala/scala/blob/2.12.x/src/library/scala/concurrent/impl/Promise.scala]]
 * - Scala 2.13.xx: See method `concatCallbacks` in [[https://github.com/scala/scala/blob/2.13.x/src/library/scala/concurrent/impl/Promise.scala]]
 */
object CallbackOrderScalaFP extends App {
  val ex = Executors.newSingleThreadExecutor()
  implicit val ec = ExecutionContext.fromExecutorService(ex)

  val p = Promise[Int]

  def callback(v: Int, s: SyncVar[List[Int]]) {
    val updated = s.take()
    s.put(updated :+ v)
  }

  val s = new SyncVar[List[Int]]
  s.put(List())

  1 to 10 foreach (i => p.future.onComplete(t => callback(i, s)))

  p.success(1)
  Await.ready(p.future, Duration.Inf)
  println("Callback execution order for Scala FP: " + s.get)
  ex.shutdown()
}