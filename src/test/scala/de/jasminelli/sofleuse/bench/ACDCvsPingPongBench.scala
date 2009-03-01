package de.jasminelli.sofleuse.bench
/**
 * TCompare ACDCBenc againts PingPongBench 
 * 
 * @author Stefan Plantikow<plantikow@googlemail.com>
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:12:28
 */
object ACDCvsPingPongBench {
  val warmup = 3 // >=1 to ensure enough threads are started
  val tries = 27 // median of tries is result, each try is n requests over given stages

  def runOnce(tag: String, cores: Int, dur: Long, stages: Int, rq: Int) = {
    // cores-parameter      load
    // >0                   NB cores (if rq < cores => bulk)
    // =0                   NB stages (if rq < stages => bulk)
    // -1                   lin
    // -2                   bulk
    val load = if (cores >= 0) {
      val partitions = if (cores == 0) stages else cores    
     if (rq < partitions) BulkRqLoad(rq) else NBParRqLoad(rq, partitions)
    }
    else {
      if (cores == -1) LinRqLoad(rq) else BulkRqLoad(rq)
    }

    //val verific = new CountingVerificator
    val verific = new PlainVerificator

    (Symbol(tag) match {
      case 'acdc =>
          new ACDCBench(BenchParams(load, dur, stages, warmup, tries, false), verific);
      case 'acdc_r =>
          new ACDCBench(BenchParams(load, dur, stages, warmup, tries, true), verific);
      case 'ppng =>
        new PingPongBench(BenchParams(load, dur, stages, warmup, tries, false), verific)
      case 'ppng_r =>
        new PingPongBench(BenchParams(load, dur, stages, warmup, tries, true), verific)
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
      // number of requests per try
      val rq = Integer.parseInt(args(2))
      // number of stages
      val stages =
        if (cores < 0)
          List(1, 2, 3, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 48, 56, 64)
        else
          List(1, 2, 3, 4, 6, 8, 10, 12, 14, 16, 20, 24, 28, 32, 48, 64, 80, 96, 128)
      for (stageParam <- stages)
      {
        Console.format("acdc %s %s %s %s\n", cores, dur, stageParam, rq)
        // Console.format("acdc_r %s %s %s %s\n", cores, dur, stages, rq)
        Console.format("ppng %s %s %s %s\n", cores, dur, stageParam, rq)
        // Console.format("ppng_r %s %s %s %s\n", cores, dur, stages, rq)
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