package main.scala.futuresandpromises.standardlibrary

import main.scala.futuresandpromises.Try

class ScalaFPTry[T](val t: scala.util.Try[T]) extends Try[T] {

}