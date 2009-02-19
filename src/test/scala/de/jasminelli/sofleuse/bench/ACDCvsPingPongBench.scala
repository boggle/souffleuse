package de.jasminelli.sofleuse.bench
/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:12:28
 */
object ACDCvsPingPongBench {
  val warmup = 1
  val tries = 9

  def runOnce(tag: String, cores: Int, dur: Long, stages: Int, rq: Int) = {
    val partitions = cores * 2
    val load = if (rq < partitions) BulkRqLoad(rq) else NBParRqLoad(rq, partitions)
    if ("acdc" == tag)
        new ACDCBench(BenchParams(load, dur, stages, warmup, tries)).generateResult(tag);
    else if ("ppng" == tag)
      new PingPongBench(BenchParams(load, dur, stages, warmup, tries)).generateResult(tag);
    else
      throw new IllegalArgumentException("Oops")
  }

  def main(args: Array[String]): Unit = {
    if (args.length == 3) {
      val cores = Integer.parseInt(args(0))
      val dur = java.lang.Long.parseLong(args(1))
      val rq = Integer.parseInt(args(2))
      for (stages <- List(1, 2, 4, 8, 12, 16, 20, 24, 28, 32))
      {
        Console.format("acdc %s %s %s %s\n", cores, dur, stages, rq)
        Console.format("ppng %s %s %s %s\n", cores, dur, stages, rq)
      }
    } else
      {
        
        runOnce(args(0), Integer.parseInt(args(1)),  java.lang.Long.parseLong(args(2)),
          Integer.parseInt(args(3)), Integer.parseInt(args(4)))
      }
  }
    // new ACDCBench(BenchParams(LinRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");
    // new PingPongBench(BenchParams(LinRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");

    // new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-bulk");
    // new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-bulk");
}