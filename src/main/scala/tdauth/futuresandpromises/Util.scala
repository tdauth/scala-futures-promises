package tdauth.futuresandpromises

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

import scala.collection.immutable.Vector
import scala.util.control.NonFatal

trait Util {
  def factory: Factory

  // Derived methods:
  def async[T](ex: Executor, f: () => T): Future[T] = {
    val p = factory.createPromise[T]
    val result = p.future()
    factory.assignExecutorToFuture(result, ex)

    ex.submit(() => {
      try {
        val result = f()
        p.trySuccess(result)
      } catch {
        case NonFatal(e) => p.tryFailure(e)
      }
    })

    result
  }

  type FirstNResultType[T] = Vector[Tuple2[Int, Try[T]]]

  def firstN[T](futures: Vector[Future[T]], n: Integer): Future[FirstNResultType[T]] = {
    class FirstNContext {
      var v: FirstNResultType[T] = Vector()
      val completed = new AtomicInteger(0)
      val done = new AtomicBoolean(false)
      val p = factory.createPromise[FirstNResultType[T]]
    }

    val ctx = new FirstNContext

    if (futures.size < n) {
      ctx.p.tryFailure(new RuntimeException("Not enough futures"))
    } else {
      for ((f, i) <- futures.view.zipWithIndex) {
        f.onComplete((t: Try[T]) => {
          if (!ctx.done.get) {
            val c = ctx.completed.incrementAndGet

            if (c <= n) {

              ctx.v.synchronized {
                ctx.v = ctx.v :+ (i, t)
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

  def firstNSucc[T](futures: Vector[Future[T]], n: Integer): Future[FirstNSuccResultType[T]] = {
    class FirstNSuccContext {
      var v: FirstNSuccResultType[T] = Vector()
      val succeeded = new AtomicInteger(0)
      val failed = new AtomicInteger(0)
      val done = new AtomicBoolean(false)
      val p = factory.createPromise[FirstNSuccResultType[T]]
    }

    val ctx = new FirstNSuccContext
    val total = futures.size

    if (total < n) {
      ctx.p.tryFailure(new RuntimeException("Not enough futures"))
    } else {
      for ((f, i) <- futures.view.zipWithIndex) {
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
                ctx.v.synchronized {
                  ctx.v = ctx.v :+ (i, t.get)
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