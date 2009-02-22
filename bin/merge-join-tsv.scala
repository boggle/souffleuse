#!/bin/sh
exec scala -nocompdaemon -savecompiled $0 $@
!#


import scala.io.Source.{fromFile, fromInputStream}
import scala.util.Sorting


/**
 * Call with <column> <list-of-files> ('-' for stdin)
 *
 * Selects a single line for each set of aligned lines from a set of tsv files based on the smallest
 * value in the given column of lines in a line set
 *
 * Files must be aligned line-wise.  A commment ('#') line in any file causes the corresponding lines in other files
 * to be ignored.  Processing stops as soon as the first input source has been read completely.
 *
 * @author Stefan Plantikow <stefan.plantikow@googlemail.com>
 * 
 */
class Merger(iters: Array[Iterator[String]], index: Int) {

  def findBest(cands: Array[String]): String = {
    Sorting.quickSort[String](cands)({ (l1: String) => new Ordered[String] {
      def compare(l2: String): Int = column(l1) - column(l2)
    }})
    cands(0)
  }

  def column(l: String): Int = Integer.parseInt((l.split('\t')(index)).trim)

  def isComment(s: String): Boolean = s.trim.startsWith("#")

  def loop: Unit = doDropWhile match {
      case None => return
      case Some(result: Array[String]) => { Console.print(findBest(result)); loop }
  }

  def doDropWhile: Option[Array[String]] = {
    if (iters.elements.forall { iter => iter.hasNext }) {
      val elements = iters.map { iter => iter.next }
      if (elements.exists(isComment))
        doDropWhile
      else
        Some(elements)
    }
    else
      None
  }
}

if (args.length <= 2) {
  Console.println("Call with <column> <list-of-files> ('-' for stdin)")
  Console.println
  Console.println("Selects a single line for each set of aligned lines from a set of tsv files based on the smallest " +
                  "value in the given column of lines in a line set")
  Console.println
  Console.print("Files must be aligned line-wise.  A commment ('#') line in any file causes the corresponding lines "
              + "in other files to be ignored.  ")
  Console.println("Processing stops as soon as the first input source has been read completely.")
  exit(1)
}
else  {
  val iters = args.subArray(1, args.length).map
                { fname => (if (fname.trim == "-") fromInputStream(System.in) else fromFile(fname)).getLines }
  val column = Integer.parseInt(args(0))
  new Merger(iters, column).loop
  exit(0)
}
