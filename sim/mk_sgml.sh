#!/bin/sh

cd doc
zip ../jedit-sgml.zip *.sgml \
	collateindex.pl \
	dsssl/*.dsl \
	images/*.gif \
	makefile.jmk
