package tdauth.futuresandpromises.mvar

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Promise

class MVarFactory extends Factory {
  override def createPromise[T](ex: Executor): Promise[T] = new MVarPromise[T](ex)
}