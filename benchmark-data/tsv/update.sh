#!/bin/sh
for i in *.tsv ; do
	./plot.sh `basename $i .tsv`
done
