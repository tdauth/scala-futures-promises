package tdauth.futuresandpromises.standardlibrary

import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext

import tdauth.futuresandpromises.AbstractPromiseTest
import tdauth.futuresandpromises.Promise

class PromiseTest extends AbstractPromiseTest {
  override def getTestName: String = "ScalaFPPromise"
  override def getPromise: Promise[Int] = new ScalaFPPromise[Int](executor)

  private val executor = new ScalaFPExecutor(ExecutionContext.fromExecutorService(Executors.newSingleThreadExecutor()))
}