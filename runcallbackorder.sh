#!/bin/bash
sbt "runMain tdauth.futuresandpromises.callbackorder.CallbackOrderPrimCas"
sbt "runMain tdauth.futuresandpromises.callbackorder.CallbackOrderScalaFP"
sbt "runMain tdauth.futuresandpromises.callbackorder.CallbackOrderTwitterUtil"
