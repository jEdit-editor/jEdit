#!/bin/sh

# This script must be run from the jEdit directory, *not* the installer
# directory!!!

# jedit-program fileset
echo jedit.jar > installer/jedit-program
echo jars/LatestVersion.jar >> installer/jedit-program
echo properties/example.props >> installer/jedit-program
find modes -name \*.xml >> installer/jedit-program
echo modes/catalog >> installer/jedit-program
find doc -type f -name \*.txt >> installer/jedit-program

echo -n "jedit-program: "
ls -l `cat installer/jedit-program` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-macros fileset
find macros -name \*.bsh > installer/jedit-macros

echo -n "jedit-macros: "
ls -l `cat installer/jedit-macros` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-firewall fileset (Java 2 only)
echo jars/Firewall.jar > installer/jedit-firewall
echo -n "jedit-firewall: "
ls -l `cat installer/jedit-firewall` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-windows fileset
echo > installer/jedit-windows

echo -n "jedit-windows: "
ls -l `cat installer/jedit-windows` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'


# jedit-os2 fileset
echo jedit.cmd > installer/jedit-os2

echo -n "jedit-os2: "
ls -l `cat installer/jedit-os2` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'


for file in installer/jedit-*
do
	sort $file > $file.tmp
	mv $file.tmp $file
done
