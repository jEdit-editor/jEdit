#!/bin/sh

cd ..

cat jEdit/sim/jedit-* | awk '{print "jEdit/"$1}' > file_list
echo jEdit/sim/* >> file_list
