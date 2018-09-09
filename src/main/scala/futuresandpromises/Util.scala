package tdauth.futuresandpromises

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

import scala.collection.immutable.Vector
import scala.util.control.NonFatal

trait Util {
  def async[T](ex: Executor, f: () => T): Future[T]
  def factory: Factory

  // Derived methods:
  type FirstNResultType[T] = Vector[Tuple2[Int, Try[T]]]

  def firstN[T](c: Vector[Future[T]], n: Integer): Future[FirstNResultType[T]] = {
    final class FirstNContext {
      // TODO Use pre allocated space of n and a more efficient way than locking.
      var l = new ReentrantLock()
      var v: FirstNResultType[T] = Vector()
      val completed = new AtomicInteger(0)
      val done = new AtomicBoolean(false)
      val p = factory.createPromise[FirstNResultType[T]]
    }

    val ctx = new FirstNContext

    if (c.size < n) {
      ctx.p.tryFailure(new RuntimeException("Not enough futures"))
    } else {
      for ((f, i) <- c.view.zipWithIndex) {
        f.onComplete((t: Try[T]) => {
          if (!ctx.done.get) {
            val c = ctx.completed.incrementAndGet

            if (c <= n) {
              ctx.l.lock()
              try {
                ctx.v = ctx.v :+ (i, t)
              } finally {
                ctx.l.unlock()
              }

              if (c == n) {
                ctx.p.trySuccess(ctx.v)
                ctx.done.set(true)
              }
            }
          }
        })
      }
    }

    ctx.p.future
  }

  type FirstNSuccResultType[T] = Vector[Tuple2[Int, T]]

  def firstNSucc[T](c: Vector[Future[T]], n: Integer): Future[FirstNSuccResultType[T]] = {
    final class FirstNSuccContext {
      // TODO Use pre allocated space of n and a more efficient way than locking.
      var l = new ReentrantLock()
      var v: FirstNSuccResultType[T] = Vector()
      val succeeded = new AtomicInteger(0)
      val failed = new AtomicInteger(0)
      val done = new AtomicBoolean(false)
      val p = factory.createPromise[FirstNSuccResultType[T]]
    }

    val ctx = new FirstNSuccContext
    val total = c.size

    if (total < n) {
      ctx.p.tryFailure(new RuntimeException("Not enough futures"))
    } else {
      for ((f, i) <- c.view.zipWithIndex) {
        f.onComplete((t: Try[T]) => {
          if (!ctx.done.get) {
            // ignore exceptions until as many futures failed that n futures cannot be completed successfully anymore
            if (t.hasException) {
              val c = ctx.failed.incrementAndGet

              /*
							 * Since the local variable can never have the counter incremented by more than one,
							 * we can check for the exact final value and do only one setException call.
							 */
              if (total - c + 1 == n) {
                try {
                  t.get
                } catch {
                  case NonFatal(e) => ctx.p.tryFailure(e)
                }

                ctx.done.set(true)
              }
            } else {
              val c = ctx.succeeded.incrementAndGet

              if (c <= n) {
                ctx.l.lock()
                try {
                  ctx.v = ctx.v :+ (i, t.get)
                } finally {
                  ctx.l.unlock()
                }

                if (c == n) {
                  ctx.p.trySuccess(ctx.v)
                  ctx.done.set(true)
                }
              }
            }
          }
        })
      }
    }

    ctx.p.future
  }
}