#!/bin/sh
exec scala -nocompdaemon -savecompiled $0 $@
!#
object HelloWorld {
  def main(args: Array[String]) {
    println("Hello, world! " + args.toList)
  }
}
HelloWorld.main(args)
