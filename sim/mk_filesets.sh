#!/bin/sh

# jedit-program fileset
# includes jedit.jar and *.txt docs

echo jedit.jar > sim/jedit-program
echo jars/LatestVersion.jar >> sim/jedit-program
find doc -type f -name \*.txt >> sim/jedit-program

echo -n "jedit-program: "
ls -l `cat sim/jedit-program` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-html fileset

find doc/jeditdocs -type f -name \*.html -print > sim/jedit-html
find doc/images -type f -name \*.gif -print >> sim/jedit-html

echo -n "jedit-html: "
ls -l `cat sim/jedit-html` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-pdf fileset

echo doc/jeditdocs.pdf > sim/jedit-pdf

echo -n "jedit-pdf: "
ls -l doc/jeditdocs.pdf | awk '{print $5 / 1024}'

# jedit-plugins fileset

find jars -type f -name \*.jar \! -name LatestVersion.jar > sim/jedit-plugins

echo -n "jedit-plugins: "
ls -l `cat sim/jedit-plugins` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-source fileset

find gnu -type f -name \*.java -print > sim/jedit-source
find org -type f \( -name \*.java -o -name \*.gif -o -name \*.props \) >> sim/jedit-source
find jars/LatestVersion -type f -print >> sim/jedit-source
echo makefile.jmk >> sim/jedit-source

echo -n "jedit-source: "
ls -l `cat sim/jedit-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-sgml fileset

echo doc/jeditdocs.sgml > sim/jedit-sgml
find doc/scripts -type f \! -path \*CVS\* -print >> sim/jedit-sgml
find doc/dsssl -type f -name \*.dsl -print >> sim/jedit-sgml
echo doc/makefile.jmk >> sim/jedit-sgml
find doc/images -type f \! -path \*CVS\* -print >> sim/jedit-sgml

echo -n "jedit-sgml: "
ls -l `cat sim/jedit-sgml` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-plugin-source fileset

find jars -type f \! -path \*LatestVersion\* \! -name \*.jar -print \
	> sim/jedit-plugin-source

echo -n "jedit-plugin-source: "
ls -l `cat sim/jedit-plugin-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-api fileset

find doc/api -type f -print > sim/jedit-api

echo -n "jedit-api: "
ls -l `cat sim/jedit-api` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-cvs fileset

find doc -path \*CVS\* -type f -print > sim/jedit-cvs
find org -path \*CVS\* -type f -print >> sim/jedit-cvs

echo -n "jedit-cvs: "
ls -l `cat sim/jedit-cvs` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-sim fileset

find sim -type f -print > sim/jedit-sim

echo -n "jedit-sim: "
ls -l `cat sim/jedit-sim` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

for file in sim/jedit-*
do
	sort $file > $file.tmp
	mv $file.tmp $file
done
