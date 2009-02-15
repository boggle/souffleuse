package de.jasminelli.sofleuse.bench
/**
 * ThingAMagic.
 * 
 * @author Stefan Plantikow<plantikow@zib.de> 
 *
 * Originally created by User: stepn Date: 15.02.2009 Time: 15:12:28
 */
object ACDCvsPingPongBench {

  def main(args: Array[String]): Unit = {
    val stages = 2
    val rq = 1024 // 512*8

    new PingPongBench(BenchParams(LinRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");

    new ACDCBench(BenchParams(LinRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");


    // new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");


    // new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");

    // new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-lin");

    //new ACDCBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("acdc-bulk");
    //new ACDCBench(BenchParams(ParRqLoad(rq, rq/stages), stages, 2, 8)).generateResult("acdc-par");

    //new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-bulk");
    //new PingPongBench(BenchParams(ParRqLoad(rq, rq/stages), stages, 2, 8)).generateResult("pingpong-par");

    //new PingPongBench(BenchParams(LinRqLoad(rq), stages, 2, 8)).generateResult("pingpong-lin");
    //new PingPongBench(BenchParams(BulkRqLoad(rq), stages, 2, 8)).generateResult("pingpong-bulk");
    //new PingPongBench(BenchParams(ParRqLoad(rq, rq/stages), stages, 2, 8)).generateResult("pingpong-par");
  }
}