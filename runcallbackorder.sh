#!/bin/bash
sbt "runMain tdauth.futuresandpromises.callbackorder.CallbackOrderCCAS"
sbt "runMain tdauth.futuresandpromises.callbackorder.CallbackOrderScalaFP"
sbt "runMain tdauth.futuresandpromises.callbackorder.CallbackOrderTwitterUtil"
