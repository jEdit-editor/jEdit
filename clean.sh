#!/bin/sh

find . -name \*~ -exec rm {} \;
find . -name .\*~ -exec rm {} \;
find . -name \*.bak -exec rm {} \;
find . -name \*.orig -exec rm {} \;
find . -name \*.rej -exec rm {} \;
find . -name \#\*\# -exec rm {} \;
find . -name .\*.swp -exec rm {} \;
find org com gnu jars -name \*.class -exec rm {} \;
find . -name .\#\* -exec rm {} \;
find . -name .new\* -exec rm {} \;
find . -name .directory -exec rm {} \;
# The PDF doc generator produces PNG files from GIF files for
# inclusion into the PDF. I would use PNG throughout, but
# Java's HTML viewer doesn't support it...
rm -f doc/images/*.png
rm -f doc/HTML.index
rm -f doc/index.sgml
rm -f doc/*.{aux,tex,log}
rm -f doc/*.out
