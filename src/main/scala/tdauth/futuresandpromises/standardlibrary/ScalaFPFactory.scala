package tdauth.futuresandpromises.standardlibrary

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Promise

class ScalaFPFactory extends Factory {
  override def createPromise[T](ex: Executor): Promise[T] = new ScalaFPPromise[T](ex)
}