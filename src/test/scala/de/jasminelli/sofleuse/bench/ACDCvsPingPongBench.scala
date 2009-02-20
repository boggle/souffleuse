package de.jasminelli.sofleuse.bench
/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:12:28
 */
object ACDCvsPingPongBench {
  val warmup = 1 // >=1 to ensure enough threads are started
  val tries = 9 // median of tries is result, each try is n requests over given stages

  def runOnce(tag: String, cores: Int, dur: Long, stages: Int, rq: Int) = {
    val partitions = stages // if (cores <= 0) 1 else cores * 2
    val load = if (rq < partitions) BulkRqLoad(rq) else NBParRqLoad(rq, partitions)
    val verific = new PlainVerificator

    (Symbol(tag) match {
      case 'acdc =>
          new ACDCBench(BenchParams(load, dur, stages, warmup, tries), verific);
      case 'ppng =>
        new PingPongBench(BenchParams(load, dur, stages, warmup, tries), verific)
      case _ =>
        throw new IllegalArgumentException("Oops")
    }).generateResult(tag)
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 3) {
      // cores in this machine
      val cores = Integer.parseInt(args(0))
      // sleep per request and stage (usually 0)
      val dur = java.lang.Long.parseLong(args(1))
      // number of requests per trie
      val rq = Integer.parseInt(args(2))
      // number of stages
      for (stages <- List(1, 2, 3, 4, 8, 12, 16, 20, 24, 28, 32, 48, 64))
      {
        Console.format("acdc %s %s %s %s\n", cores, dur, stages, rq)
        Console.format("ppng %s %s %s %s\n", cores, dur, stages, rq)
      }
    }
    else {
      if (args.length == 5)
      {
        runOnce(args(0), Integer.parseInt(args(1)),  java.lang.Long.parseLong(args(2)),
          Integer.parseInt(args(3)), Integer.parseInt(args(4)))
      }
      else {
      Console.println("Either call with <cores> <sleep-dur-per-rq-in-stage> <num-requests> to " +
      "generate a sensible list of benchmark parameter trials or call with <acdc|ppng> <cores> " +
      "<sleep-dur-per-rq-in-stage> <num-stages> <num-requests> to execute the benchmark for " +
      " a single set of parameter values")
      }
    }
  }
}