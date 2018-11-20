package tdauth.futuresandpromises.example

import tdauth.futuresandpromises.Try
import tdauth.futuresandpromises.standardlibrary.ScalaFPExecutor
import tdauth.futuresandpromises.standardlibrary.ScalaFPUtil

object Example extends App {
  val executor = new ScalaFPExecutor
  val f0 = ScalaFPUtil.async(executor, () => 10).guard(_ == 10).transform((t: Try[Int]) => t.get() * 10)
  val f1 = ScalaFPUtil.async(executor, () => 11)
  val f2 = f0.first(f1)
  f2.onComplete((t: Try[Int]) => println(t.get))

  // Do some other stuff ...

  f2.get
}