#!/bin/bash
echo "$(which sbt)"
sbt assembly
sbt run &
pid="$!"
bash ./jvmtop-0.8.0/jvmtop.sh --profile "$pid"
wait "$pid"