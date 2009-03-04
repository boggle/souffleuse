#!/bin/sh  
# You might need echo -e depending on your platform
TAB=$(echo "\t")
cat $* | fgrep -v '#' |cut -f2,3,5,1,7,8,10,12,13 | xargs -L 2 /bin/echo | awk '{print $2,$3,$4,$1,$5,$6,$7,$10,$14,$15,$16,$8,$17,$9,$18;}' | sed "s/ /$TAB/g"