package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Promise
import tdauth.futuresandpromises.Try

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "ScalaFPPromise"
  override def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)
  override def getTry: Try[Int] = new ScalaFPTry[Int]

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}