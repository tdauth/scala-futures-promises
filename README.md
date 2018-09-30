# Advanced Futures and Promises with Scala

Advanced futures and promises API for Scala based on our paper [Advanced Futures and Promises in C++](http://www.home.hs-karlsruhe.de/~suma0002/publications/advanced-futures-promises-cpp.pdf).
It provides an implementation based on the Scala Standard Library for futures and promises.


The implementation with Scala is much easier than with C++ since the garbage collection makes all shared pointers for callbacks unnecessary.
Besides, better abstraction is possible since the futures and promises are heap-allocated in Scala.
Therefore, the library can provide abstract classes or traits.
This allows real abstraction for the derived methods which can already all be implemented in the traits.
Only the core methods have to be implemented by a concrete implementation.
The trait `Factory` has to be extended for creating instances of the concrete implementations.


The derived method `firstSucc` actually completes the final future with the final thrown exception if both futures fail since we cannot rely on broken promises in Scala because of the garbage collection.
For the same reason the exception type `BrokenPromise` is never used in Scala.

## Automatic Build with TravisCI
[![Build Status](https://travis-ci.org/tdauth/scala-futures-promises.svg?branch=master)](https://travis-ci.org/tdauth/scala-futures-promises)
[![Code Coverage](https://img.shields.io/codecov/c/github/tdauth/scala-futures-promises/master.svg)](https://codecov.io/github/tdauth/scala-futures-promises?branch=master)

## Manual Build
Use the command `sbt compile` to build the project manually.

## Implementation based on Scala FP
Scala provides a standard library for [futures and promises](http://docs.scala-lang.org/overviews/core/futures.html) which we will call Scala FP.
The source code of Scala FP in Scala version 2.13.x can be found [here](https://github.com/scala/scala/tree/2.13.x/src/library/scala/concurrent).
The latest API documentation can be found [here](https://www.scala-lang.org/api/current/scala/concurrent/index.html).
Futures in Scala FP can have multiple callbacks registered and have multi-read semantics.

### Usage
This basic example shows how to create futures with the Scala FP implementation: [Example.scala](./src/main/scala/tdauth/futuresandpromises/example/Example.scala)

## Implementation based on Scala FP without Derived Methods
There is another implementation located in the package [nonderived](./src/main/scala/tdauth/futuresandpromises/nonderived) which is based on the first Scala FP implementation.
However, it implements all derived methods with methods which are already provided by Scala FP if it is possible.
This implementation shows the power of Scala FP itself.
The following combinators provided by our Advanced Futures and Promises are not directly provided by Scala FP (see <https://stackoverflow.com/questions/52408674/do-scala-futures-support-for-non-blocking-combinators-such-as-firstncompletedof>):
* `Util.firstN`
* `Util.firstNSucc`
* `Promise.trySuccessWith`
* `Promise.tryFailureWith`

## Implementation based on Twitter's Futures and Promises
The package [twitter](./src/main/scala/tdauth/futuresandpromises/twitter) contans an implementation of the Advanced Futures and Promises based on [Twitter's futures and promises](https://twitter.github.io/util/).
These are similiar to Scala FP but the project Finagle existed before Scala's adaption of futures and promises.

## Implementation of missing functionality from Scala FP
Our Advanced Futures and Promises do not provide all functionality from Scala FP.
The package [comprehensive](./src/main/scala/tdauth/futuresandpromises/comprehensive) adds all missing functionality and shows that our basic combinators should be enough to implement the missing functionality except for the method `Future.value` which has to stay abstract.
The objects `ComprehensivePromise` and `ComprehensiveFuture` don't make much sense since we use the trait `Factory` for construction.
Therefore, a factory has to be passed to many methods.

## Combinators
The object [Combinators](./src/main/scala/tdauth/futuresandpromises/combinators/Combinators.scala) contains different implementations of the non-blocking combinators using each other.

## Unit Tests
The unit tests are realized with the help of [ScalaTest](http://www.scalatest.org/).
See [Using ScalaTest with sbt](http://www.scalatest.org/user_guide/using_scalatest_with_sbt) for more information about how to use it with sbt.
Use the command `sbt test` to run all unit tests.

Unit tests should use executors with only one thread to keep the order always the same.
Besides, futures should be completed with the help of promises instead of `Util.async`/submitting a function to executor, to keep the execution order the same.
For data race detections there should be different test cases/tools.

## Coverage
The project uses [scoverage](http://scoverage.org/) to generate coverage reports.
It uses the plugin [sbt-scoverage](https://github.com/scoverage/sbt-scoverage).
The command `sbt clean coverage test coverageReport` generates coverage reports into the directory `target/scala-<scala-version>/scoverage-report`.

## Performance Tests
[scalameter](https://scalameter.github.io/) is a Scala library which allows you to write performance tests in Scala.
The package [performance](./src/test/scala/tdauth/futuresandpromises/performance) contains performance tests.

## API Documentation
The API documentation can be generated with [scaladoc](https://docs.scala-lang.org/style/scaladoc.html) with the following command: `sbt doc`
It will be generated into the directory `target/scala-<scala-version>/api/`.

## Eclipse Support
Use the commnad `sbt eclipse` to generate an Eclipse project.