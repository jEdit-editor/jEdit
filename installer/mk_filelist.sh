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
find macros -name \*.macro >> installer/jedit-program

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

# jedit-source fileset

find bsh -type f \( -name \*.java -o -name \*.bsh -o -name \*.txt \) -print > installer/jedit-source
echo bsh/lib/{javaPackages,defaultImports} >> installer/jedit-source
find com -type f -name \*.java -print >> installer/jedit-source
find gnu -type f -name \*.java -print >> installer/jedit-source
find org -type f \( -name \*.java -o -name \*.bsh -o -name \*.gif -o -name \*.props -o -name \*.jpg \) >> installer/jedit-source
find jars/PluginManager -type f -print >> installer/jedit-source
find jars/LatestVersion -type f -print >> installer/jedit-source
find jars/EditBuddy -type f -print >> installer/jedit-source
echo makefile.jmk >> installer/jedit-source
echo clean.sh >> installer/jedit-source
echo org/gjt/sp/jedit/jedit.manifest >> installer/jedit-source
echo org/gjt/sp/jedit/default.abbrevs >> installer/jedit-source
echo org/gjt/sp/jedit/actions.{xml,dtd} >> installer/jedit-source

echo -n "jedit-source: "
ls -l `cat installer/jedit-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-firewall-source fileset
find jars/Firewall -type f -print > installer/jedit-firewall-source

echo -n "jedit-firewall-source: "
ls -l `cat installer/jedit-firewall-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

for file in installer/jedit-*
do
	sort $file > $file.tmp
	mv $file.tmp $file
done

# jedit-installer-source fileset

echo installer/*.sh > installer/jedit-installer-source
echo installer/makefile.jmk >> installer/jedit-installer-source
echo installer/*.java >> installer/jedit-installer-source
echo installer/install.{mf,props} >> installer/jedit-installer-source
echo installer/logo.gif >> installer/jedit-installer-source
echo installer/readme.html >> installer/jedit-installer-source

echo -n "jedit-installer-source: "
ls -l `cat installer/jedit-installer-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

cat /dev/null > installer/file_list

for file in installer/jedit-*
do
	echo FILESET_`basename $file` >> installer/file_list
	cat $file >> installer/file_list
done

export CLASSPATH=$CLASSPATH:installer
java Archive c installer/install.dat `cat installer/file_list` > /dev/null
