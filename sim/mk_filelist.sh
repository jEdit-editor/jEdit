#!/bin/sh

cd ..

cat jEdit-2.3pre5/sim/jedit-* | awk '{print "jEdit-2.3pre5/"$1}' > file_list
echo jEdit-2.3pre5/sim/* >> file_list
