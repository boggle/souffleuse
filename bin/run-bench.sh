TAG=$1-`date +%Y-%H-%M`
sh bin/run-single-bench.sh 1 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 2 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 4 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 8 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 10 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 12 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 16 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh 0 0 65535 | tee benchmark-data/BENCHMARK.$TAG
sh bin/run-single-bench.sh -1 0 65535 | tee benchmark-data/BENCHMARK.$TAG-