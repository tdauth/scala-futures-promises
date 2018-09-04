package main.scala.futuresandpromises

import scala.util.control.NonFatal

trait Future[T] {
  def get: T
  def isReady: Boolean
  def then[S](f: (Try[T]) => S): Future[S]

  // This method is required since Promise is defined as trait:
  def createPromise: Promise[T]

  // Derived methods:
  def onComplete(f: (Try[T]) => Unit): Unit = {
    this.then(f)
  }

  def guard(f: (T) => Boolean): Future[T] = {
    return this.then[T]((t: Try[T]) => {
      val v: T = t.get()

      if (!f(v)) {
        throw new PredicateNotFulfilled
      }

      v
    }: T)
  }

  def orElse(other: Future[T]): Future[T] = {
    return this.then[T]((t: Try[T]) => {
      if (t.hasException) {
        try {
          other.get
        } catch {
          case NonFatal(x) => t.get // will rethrow if failed
        }
      } else {
        t.get // will rethrow if failed
      }
    }: T)
  }

  def first(other: Future[T]): Future[T] = {
    val p = createPromise

    this.onComplete((t: Try[T]) => {
      p.tryComplete(t)
    })

    other.onComplete((t: Try[T]) => {
      p.tryComplete(t)
    })

    p.future
  }

  def firstSucc(other: Future[T]): Future[T] = {
    val p = createPromise

    this.onComplete((t: Try[T]) => {
      p.trySuccess(t.get())
    })

    other.onComplete((t: Try[T]) => {
      p.trySuccess(t.get())
    })

    p.future
  }
}