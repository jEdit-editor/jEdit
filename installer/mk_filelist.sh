#!/bin/sh

# This script must be run from the jEdit directory, *not* the installer
# directory!!!

# jedit-program fileset
# includes jedit.jar and *.txt docs

echo jedit.jar > installer/jedit-program
echo jars/PluginManager.jar >> installer/jedit-program
echo jars/LatestVersion.jar >> installer/jedit-program
echo jars/EditBuddy.jar >> installer/jedit-program
echo site-props/*.props >> installer/jedit-program
find modes -name \*.xml >> installer/jedit-program
find modes -name \*.dtd >> installer/jedit-program
find doc -type f -name \*.txt >> installer/jedit-program
find macros -name \*.bsh >> installer/jedit-program

echo -n "jedit-program: "
ls -l `cat installer/jedit-program` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-firewall fileset (Java 2 only)

echo jars/Firewall.jar > installer/jedit-firewall
echo -n "jedit-firewall: "
ls -l `cat installer/jedit-firewall` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-html fileset

find doc/users-guide -type f -name \*.html -print > installer/jedit-html

echo -n "jedit-html: "
ls -l `cat installer/jedit-html` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

for file in installer/jedit-*
do
	sort $file > $file.tmp
	mv $file.tmp $file
done
