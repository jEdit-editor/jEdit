#!/bin/sh

cd doc
zip ../jedit-sgml.zip jeditdocs.sgml \
	collateindex.pl \
	dsssl/*.dsl \
	images/*.gif \
	makefile.jmk
