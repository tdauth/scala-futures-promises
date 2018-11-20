package tdauth.futuresandpromises.nonderived

import scala.util.Failure
import scala.util.Success
import scala.util.control.NonFatal

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.PredicateNotFulfilled
import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPFuture

class NonDerivedFuture[T](val future: scala.concurrent.Future[T], ex: Executor) extends ScalaFPFuture[T](future, ex) {

  // Basic methods:
  override def factory: Factory = new NonDerivedFactory

  // Derived methods:
  override def transform[S](f: (Try[T]) => S): Future[S] = new NonDerivedFuture[S](future.transform[S]((t: scala.util.Try[T]) => {
    try {
      Success(f.apply(new Try[T](t)))
    } catch {
      case NonFatal(e) => Failure(e)
    }
  })(this.ex.asInstanceOf[NonDerivedExecutor].executionContext), this.getExecutor)

  override def transformWith[S](f: (Try[T]) => Future[S]): Future[S] = new NonDerivedFuture[S](future.transformWith[S]((t: scala.util.Try[T]) => {
    f.apply(new Try[T](t)).asInstanceOf[NonDerivedFuture[S]].future
  })(this.ex.asInstanceOf[NonDerivedExecutor].executionContext), this.getExecutor)

  override def guard(f: (T) => Boolean): Future[T] = new NonDerivedFuture[T](future.filter(f)(this.ex.asInstanceOf[NonDerivedExecutor].executionContext).recover({ case e: NoSuchElementException => throw new PredicateNotFulfilled })(this.ex.asInstanceOf[NonDerivedExecutor].executionContext), this.ex)

  override def orElse(other: Future[T]): Future[T] = new NonDerivedFuture[T](future.fallbackTo(other.asInstanceOf[NonDerivedFuture[T]].future), this.ex)

  override def first(other: Future[T]): Future[T] = new NonDerivedFuture[T](scala.concurrent.Future.firstCompletedOf(Vector(this.future, other.asInstanceOf[NonDerivedFuture[T]].future))(this.ex.asInstanceOf[NonDerivedExecutor].executionContext), this.ex)

  /**
   * Has to be implemented manually with the help of Future.find since there is no such method in Scala FP.
   * TODO #17 Can it even be implemented with Future.find?
   * Apparently, the implementation looks like the search is done sequentially, waiting for one future, after another.
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
    val executionContext = this.ex.asInstanceOf[NonDerivedExecutor].executionContext
    val f0 = this.future.transform(callback)(executionContext)
    val f1 = other.asInstanceOf[NonDerivedFuture[T]].future.transform(callback)(executionContext)
    val vector = Vector(f0, f1)
    new NonDerivedFuture[T](scala.concurrent.Future.find(vector)(_ => true)(executionContext).transform((t: scala.util.Try[Option[T]]) => {
      val o = t.get
      o match {
        case Some(v) => scala.util.Success(v)
        case None => scala.util.Failure(ctx.e)
      }
    })(executionContext), this.ex)
  }
}