#!/bin/sh
cat plot | sed -e"s/\$1/$1/g" | gnuplot -
