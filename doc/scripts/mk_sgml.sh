#!/bin/sh

# This script creates the HTML and PDF docs.
# It should be run from the root directory of the jEdit distribution.

cd doc

# Create index
touch index.sgml
jade -t sgml -d dsssl/jedithtml.dsl -V html-index jeditdocs.sgml
perl scripts/collateindex.pl -i word-index -g -p -f \
	-o index.sgml HTML.index
rm *.html -f

# Create HTML docs
sgmltools --dsssl-spec=dsssl/jedithtml.dsl --backend=html jeditdocs.sgml

# Create PDF docs

# pdftex only supports PNG (and I would use PNG in the HTML if Swing
# supported it)
for file in images/*.gif
do
	convert $file images/`basename $file .gif`.png
done

jade -d dsssl/jeditprint.dsl -t tex jeditdocs.sgml
pdftex "&jadetexpdf" jeditdocs.tex
pdftex "&jadetexpdf" jeditdocs.tex
pdftex "&jadetexpdf" jeditdocs.tex
