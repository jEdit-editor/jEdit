#!/bin/sh

# Creates the jEdit source tarball.

if [ "$1" = "" ]; then
  echo "Must specify a command line parameter."
  exit 1
fi

sh clean.sh

cd ..
tar cvfz jedit${1}source.tar.gz `find jEdit-3.0.2 -type f \! \( -name Entries \
	-o -name Root -o -name Entries.Static -o -name Repository \
	-o -name \*.class -o -name \*.jar -o -name install.dat \
	-o -name jedit-\* \)`
