package tdauth.futuresandpromises.standardlibrary

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.util.control.NonFatal

import tdauth.futuresandpromises.Future
import tdauth.futuresandpromises.Try

class ScalaFPFuture[T](f: scala.concurrent.Future[T]) extends Future[T] {

  override def get: T = {
    Await.result(f, Duration.Inf)
  }

  override def isReady: Boolean = {
    f.isCompleted
  }

  override def then[S](callback: Try[T] => S): Future[S] = {
    val transformCallback: (scala.util.Try[T]) => scala.util.Try[S] = (t: scala.util.Try[T]) => {
      val transformedTry = new ScalaFPTry(t)
      try {

        val callbackResult = callback(transformedTry)
        scala.util.Success(callbackResult)
      } catch {
        case NonFatal(e) => scala.util.Failure(e)
      }
    }
    val ex = new ScalaFPExecutor
    val executionContext = ex.executionContext
    val resultFuture: scala.concurrent.Future[S] = f.transform(transformCallback)(executionContext)

    new ScalaFPFuture[S](resultFuture)
  }

  override def factory = new ScalaFPFactory
}