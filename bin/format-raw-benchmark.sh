#!/bin/sh
cat $* | fgrep -v '#' | xargs -L 2 /bin/echo | sed 's/ /	/g' | cut -d '	' -f2,3,1,6,8,13
