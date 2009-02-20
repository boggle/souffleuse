#!/bin/sh
CLASSPATH=`for i in $SCALA_HOME/lib/*.jar ; do /bin/echo -n $i:; done`
JVMARGS="-server"
java $JVMARGS -cp target/classes:target/test-classes:$CLASSPATH de.jasminelli.sofleuse.bench.ACDCvsPingPongBench $* | xargs -n 5 java $JVMARGS -cp target/classes:target/test-classes:$CLASSPATH de.jasminelli.sofleuse.bench.ACDCvsPingPongBench
