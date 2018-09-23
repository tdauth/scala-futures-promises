package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.PredicateNotFulfilled
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture
import scala.util.Failure

class NonDerivedFuture[T](future: scala.concurrent.Future[T], ex: NonDerivedExecutor) extends ScalaFPFuture[T](future, ex) {

  // Basic methods:
  override def factory: Factory = new NonDerivedFactory

  // Derived methods:
  override def onComplete(f: (Try[T]) => Unit): Unit = future.onComplete((t: scala.util.Try[T]) => f(new NonDerivedTry[T](t)))(this.ex.executionContext)

  override def guard(f: (T) => Boolean): Future[T] = new NonDerivedFuture[T](future.filter(f)(this.ex.executionContext).recover({ case e: NoSuchElementException => throw new PredicateNotFulfilled })(this.ex.executionContext), this.ex)

  override def orElse(other: Future[T]): Future[T] = new NonDerivedFuture[T](future.fallbackTo(other.asInstanceOf[NonDerivedFuture[T]].future), this.ex)

  override def first(other: Future[T]): Future[T] = new NonDerivedFuture[T](scala.concurrent.Future.firstCompletedOf(Vector(this.future, other.asInstanceOf[NonDerivedFuture[T]].future))(this.ex.executionContext), this.ex)

  /**
   * Has to be implemented manually with the help of Future.find since there is no such method in Scala FP.
   */
  override def firstSucc(other: Future[T]): Future[T] = {
    /*
     * The shared context is required to store the final thrown exception and to rethrow it.
     * Otherwise, it would get lost.
     */
    class Context(var e: Throwable = null)
    val ctx = new Context
    val callback = (t: scala.util.Try[T]) => {
      t match {
        case scala.util.Success(v) => scala.util.Success(v)
        case scala.util.Failure(e) => {
          ctx.synchronized { ctx.e = e }
          scala.util.Failure(e)
        }
      }
    }
    val f0 = this.future.transform(callback)(this.ex.executionContext)
    val f1 = other.asInstanceOf[NonDerivedFuture[T]].future.transform(callback)(this.ex.executionContext)
    val vector = Vector(f0, f1)
    new NonDerivedFuture[T](scala.concurrent.Future.find(vector)(_ => true)(this.ex.executionContext).transform((t: scala.util.Try[Option[T]]) => {
      val o = t.get
      o match {
        case Some(v) => scala.util.Success(v)
        case None => scala.util.Failure(ctx.e)
      }
    })(this.ex.executionContext), this.ex)
  }
}