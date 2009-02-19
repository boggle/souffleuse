#!/bin/sh
CLASSPATH=`for i in $SCALA_HOME/lib/*.jar ; do /bin/echo -n $i:; done`
java -server -cp target/classes:target/test-classes:$CLASSPATH de.jasminelli.sofleuse.bench.ACDCvsPingPongBench $* | xargs -n 5 java -server -cp target/classes:target/test-classes:$CLASSPATH de.jasminelli.sofleuse.bench.ACDCvsPingPongBench
