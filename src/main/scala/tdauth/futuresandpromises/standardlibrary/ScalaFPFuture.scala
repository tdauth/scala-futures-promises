package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class ScalaFPFuture[T](val future: scala.concurrent.Future[T], var ex: ScalaFPExecutor) extends Future[T] {

  override def get: T = Await.result(future, Duration.Inf)

  override def isReady: Boolean = future.isCompleted

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
    new ScalaFPFuture[S](future.transform[S](transformCallback)(ex.executionContext), ex)
  }

  override def sync: Unit = Await.ready(future, Duration.Inf)

  override def factory: Factory = new ScalaFPFactory
}