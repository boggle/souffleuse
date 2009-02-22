#!/bin/sh
TAG=$1-`date +%Y-%m-%d-%H:%M`
bin/run-single-bench.sh -2 0 65536 | tee benchmark-data/BENCHMARK.$TAG-BLK
bin/run-single-bench.sh -1 0 65536 | tee benchmark-data/BENCHMARK.$TAG-LIN
bin/run-single-bench.sh 0 0 65536 | tee benchmark-data/BENCHMARK.$TAG-STG
bin/run-single-bench.sh 1 0 65536 | tee benchmark-data/BENCHMARK.$TAG-001
bin/run-single-bench.sh 2 0 65536 | tee benchmark-data/BENCHMARK.$TAG-002
bin/run-single-bench.sh 4 0 65536 | tee benchmark-data/BENCHMARK.$TAG-004
bin/run-single-bench.sh 8 0 65536 | tee benchmark-data/BENCHMARK.$TAG-008
bin/run-single-bench.sh 10 0 65536 | tee benchmark-data/BENCHMARK.$TAG-010
bin/run-single-bench.sh 12 0 65536 | tee benchmark-data/BENCHMARK.$TAG-012
bin/run-single-bench.sh 16 0 65536 | tee benchmark-data/BENCHMARK.$TAG-016
