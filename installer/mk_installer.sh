#!/bin/sh

# Creates the installer JAR. Must be run from the installer directory.
# A command line parameter must be specified, with the jEdit version
# number.

if [ "$1" = "" ]; then
  echo "Must specify a command line parameter."
  exit 1
fi

# By default, put it in your home directory, because that's what I do.
DESTDIR=$HOME

jar cfm $DESTDIR/jedit$1install.jar install.mf install.props install.dat \
	readme.html logo.gif *.class
