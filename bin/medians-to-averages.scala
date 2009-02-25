#!/bin/sh
exec scala -nocompdaemon -savecompiled $0 $@
!#


import scala.io.Source.{fromFile, fromInputStream}
import scala.util.Sorting

/**
 *
 * @author Stefan Plantikow <stefan.plantikow@googlemail.com>
 * 
 */
class Converter(sumColumn: Int, numColumn: Int, tgtColumn: Int, lines: Iterator[String]) {
                                           
  implicit def str2int(s: String): Int = Integer.parseInt(s)
  implicit def int2str(i: Int): String = i.toString


  def process(line: String) = {
	var cols = line.split('\t')   
	val newVal = cols(sumColumn) / cols(numColumn)
	if (newVal != (cols(tgtColumn): Int))
		Console.println("oops " + (cols(tgtColumn)-newVal))
	cols(tgtColumn) = newVal
	(for (col <- cols)	
		yield (col + "\t")).reduceLeft(_+_)
  }

  def isComment(s: String): Boolean = {
	val trimmed = s.trim
	trimmed.length == 0 || trimmed.startsWith("#")
  }

  def loop: Unit =
	for (line <- lines)
		Console.println(if (isComment(line)) line else process(line))
}

if (args.length != 4) {
  Console.println("Call with <sum-column> <num-column> <tgt-column> <file> ('-' for stdin)")
  Console.println
  Console.println("Replaces tgt-column with sum-colum/num-column in all lines of a file from list-of-files")
  exit(1)
}
else  {                                    
  val fname = args(3)
  val lines: Iterator[String] =  
		(if (fname.trim == "-") fromInputStream(System.in) else fromFile(fname)).getLines
  val sumColumn = Integer.parseInt(args(0))
  val numColumn = Integer.parseInt(args(1))
  val tgtColumn = Integer.parseInt(args(2))
  new Converter(sumColumn, numColumn, tgtColumn, lines).loop
  exit(0)
}
