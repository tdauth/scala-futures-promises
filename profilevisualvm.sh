#!/bin/bash
echo "$(which sbt)"
sbt assembly
java -agentpath:/home/tamino/Dokumente/visualvm_142/profiler/lib/deployed/jdk16/linux-amd64/libprofilerinterface.so=/home/tamino/Dokumente/visualvm_142/profiler/lib,5140 -jar "./target/scala-2.12/scala-futures-promises-assembly-1.0.jar"