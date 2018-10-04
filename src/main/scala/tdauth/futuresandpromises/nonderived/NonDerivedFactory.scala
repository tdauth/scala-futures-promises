package tdauth.futuresandpromises.nonderived

import tdauth.futuresandpromises.Executor
import tdauth.futuresandpromises.Factory
import tdauth.futuresandpromises.Promise

class NonDerivedFactory extends Factory {
  override def createPromise[T](ex: Executor): Promise[T] = new NonDerivedPromise[T](ex)
}