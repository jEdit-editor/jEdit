#!/bin/sh

# Creates the jEdit source tarball.

sh clean.sh

cd ..
tar cvfz jedit${1}source.tar.gz `find jEdit -type f \! \( -name Entries \
	-o -name Root -o -name Entries.Static -o -name Repository \
	-o -name \*.class -o -name \*.jar -o -name install.dat \
	-o -name file_list \)`
