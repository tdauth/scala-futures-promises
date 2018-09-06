# Scala Futures and Promises

Advanced futures and promises API for Scala based on our paper [Advanced Futures and Promises in C++](http://www.home.hs-karlsruhe.de/~suma0002/publications/advanced-futures-promises-cpp.pdf).
It provides an implementation based on the Scala Standard Library for futures and promises.
The implementation with Scala should be much easier than with C++ since the garbage collection makes all shared pointers for callbacks unnecessary.
Besides, better abstraction is possible since the futures and promises are heap-allocated in Scala.
Therefore, the library can provide abstract classes or traits.
This allows real abstraction for the derived methods which can already all be implemented in the traits.
Only the core methods have to be implemented by a concrete implementation.
The trait `Factory` has to be extended for creating instances of the concrete implementations.

## Automatic Build with TravisCI
[![Build Status](https://travis-ci.org/tdauth/scala-futures-promises.svg?branch=master)](https://travis-ci.org/tdauth/scala-futures-promises)

## Manual Build
Use the command `sbt compile` to build the project manually.

## Implementation based on Scala FP
Scala provides a standard library for [futures and promises](http://docs.scala-lang.org/overviews/core/futures.html) which we will call Scala FP.
The source code of Scala FP in Scala version 2.13.x can be found [here](https://github.com/scala/scala/tree/2.13.x/src/library/scala/concurrent).
The latest API documentation can be found [here](https://www.scala-lang.org/api/current/scala/concurrent/index.html).
Futures in Scala FP can have multiple callbacks registered and have multi-read semantics.

## Unit Tests
The unit tests are realized with the help of [ScalaTest](http://www.scalatest.org/).
See [Using ScalaTest with sbt](http://www.scalatest.org/user_guide/using_scalatest_with_sbt) for more information about how to use it with sbt.
Use the command `sbt test` to run all unit tests.

## Coverage
The project uses [scoverage](http://scoverage.org/) to generate coverage reports.
It uses the plugin [sbt-scoverage](https://github.com/scoverage/sbt-scoverage).
The command `sbt clean coverage test coverageReport` generates coverage reports into the directory `target/scala-<scala-version>/scoverage-report`.

## Eclipse Support
Use the commnad `sbt eclipse` to generate an Eclipse project.