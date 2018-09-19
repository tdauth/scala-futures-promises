package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.standardlibrary.ScalaFPPromise
import tdauth.futuresandpromises.Future

class NonDerivedPromise[T] extends ScalaFPPromise[T] {

  override def factory = new NonDerivedFactory

  // TODO Implement derived methods manually
  override def trySuccess(v: T): Boolean = {
    p.trySuccess(v)
  }

  override def tryFailure(e: Throwable): Boolean = {
    p.tryFailure(e)
  }

  override def tryCompleteWith(f: Future[T]): Unit = {
    p.tryCompleteWith(f)
  }

  override def trySuccessWith(f: Future[T]): Unit = {
    // TODO Has to be implemented manually since there is no such method in Scala
  }

  override def tryFailureWith(f: Future[T]): Unit = {
     // TODO Has to be implemented manually since there is no such method in Scala
  }
}