package de.jasminelli.sofleuse.util

import junit.framework._
import Assert._;

object AppTest {
    def suite: Test = {
        val suite = new TestSuite(classOf[AppTest]);
        suite
    }

    def main(args : Array[String]) {
        junit.textui.TestRunner.run(suite);
    }
}

/**
 * Unit test for simple App.
 */
class AppTest extends TestCase("app") {

    // Rigourous Tests :-)

    def testRpcDemo = assert(RpcDemo.rpcDemo == 2)

    def testExamplePlay = assert(ExamplePlay.whatIsSixTimesSeven == 42)
}
