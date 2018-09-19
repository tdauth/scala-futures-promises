package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.PredicateNotFulfilled
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture

class NonDerivedFuture[T](val future: scala.concurrent.Future[T], var ex: NonDerivedExecutor) extends ScalaFPFuture[T](future, ex) {

  // Basic methods:
  override def factory: Factory = new NonDerivedFactory

  // Derived methods:
  override def onComplete(f: (Try[T]) => Unit): Unit = {
    future.onComplete((t: scala.util.Try[T]) => f(new NonDerivedTry[T](t)))(this.ex.executionContext)
  }

  override def guard(f: (T) => Boolean): Future[T] = {
    new NonDerivedFuture[T](future.filter(f)(this.ex.executionContext).recover({ case e: NoSuchElementException => throw new PredicateNotFulfilled })(this.ex.executionContext), this.ex)
  }

  override def orElse(other: Future[T]): Future[T] = {
    new NonDerivedFuture[T](future.fallbackTo(other.asInstanceOf[NonDerivedFuture[T]].future), this.ex)
  }

  override def first(other: Future[T]): Future[T] = {
    new NonDerivedFuture[T](scala.concurrent.Future.firstCompletedOf(Vector(this.future, other.asInstanceOf[NonDerivedFuture[T]].future))(this.ex.executionContext), this.ex)
  }

  override def firstSucc(other: Future[T]): Future[T] = {
    // TODO implement this method manually
  }
}