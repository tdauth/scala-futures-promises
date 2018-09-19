package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class ScalaFPFuture[T](val f: scala.concurrent.Future[T], var ex: ScalaFPExecutor) extends Future[T] {

  override def get: T = Await.result(f, Duration.Inf)

  override def isReady: Boolean = f.isCompleted

  override def then[S](callback: Try[T] => S): Future[S] = {
    val transformCallback: (scala.util.Try[T]) => scala.util.Try[S] = (t: scala.util.Try[T]) => {
      try {
        scala.util.Success(callback(new ScalaFPTry(t)))
      } catch {
        case NonFatal(e) => scala.util.Failure(e)
      }
    }

    /*
     * Use the execution context of the current executor and keep the current executor for the resulting future.
     */
    new ScalaFPFuture[S](f.transform[S](transformCallback)(ex.executionContext), ex)
  }

  override def sync: Unit = Await.ready(f, Duration.Inf)

  override def factory: Factory = new ScalaFPFactory
}