#!/bin/sh
CLASSPATH=`for i in $SCALA_HOME/lib/*.jar ; do /bin/echo -n $i:; done`
JVM="$JAVA_HOME/bin/java -server"
echo "# BENCHMARK PARAMETERS: $*"
$JVM -cp target/classes:target/test-classes:$CLASSPATH de.jasminelli.souffleuse.bench.ACDCvsPingPongBench $* | xargs -n 5 $JVM -cp target/classes:target/test-classes:$CLASSPATH de.jasminelli.souffleuse.bench.ACDCvsPingPongBench
