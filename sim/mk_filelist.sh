#!/bin/sh

# jedit-program fileset
# includes jedit.jar and *.txt docs

echo jedit.jar > sim/jedit-program
echo jars/PluginManager.jar >> sim/jedit-program
echo jars/LatestVersion.jar >> sim/jedit-program
find modes -name \*.xml >> sim/jedit-program
find modes -name \*.dtd >> sim/jedit-program
find doc -type f -name \*.txt >> sim/jedit-program

echo -n "jedit-program: "
ls -l `cat sim/jedit-program` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-firewall fileset (Java 2 only)

echo jars/Firewall.jar > sim/jedit-firewall
echo -n "jedit-firewall: "
ls -l `cat sim/jedit-firewall` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-macros fileset

find macros -type f -name \*.macro > sim/jedit-macros

echo -n "jedit-macros: "
ls -l `cat sim/jedit-macros` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-html fileset

find doc/users-guide -type f -name \*.html -print > sim/jedit-html
find doc/images -type f -name \*.gif -print >> sim/jedit-html

echo -n "jedit-html: "
ls -l `cat sim/jedit-html` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-source fileset

find com -type f -name \*.java -print > sim/jedit-source
find gnu -type f -name \*.java -print >> sim/jedit-source
find org -type f \( -name \*.java -o -name \*.gif -o -name \*.props -o -name \*.jpg \) >> sim/jedit-source
find jars/PluginManager -type f -print >> sim/jedit-source
find jars/LatestVersion -type f -print >> sim/jedit-source
echo makefile.jmk >> sim/jedit-source
echo org/gjt/sp/jedit/jedit.manifest >> sim/jedit-source
echo org/gjt/sp/jedit/default.abbrevs >> sim/jedit-source

echo -n "jedit-source: "
ls -l `cat sim/jedit-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

# jedit-firewall-source fileset
find jars/Firewall -type f -print > sim/jedit-firewall-source

echo -n "jedit-firewall-source: "
ls -l `cat sim/jedit-firewall-source` | awk 'BEGIN { size=0 } { size+=$5 } END { print size / 1024 }'

for file in sim/jedit-*
do
	sort $file > $file.tmp
	mv $file.tmp $file
done

cat /dev/null > file_list

for file in sim/jedit-*
do
	echo FILESET_`basename $file` >> file_list
	cat $file >> file_list
done

echo FILESET_do_not_install_these >> file_list
echo sim/*.sh >> file_list

export CLASSPATH=$CLASSPATH:..
java org.gjt.sp.sim.Archive c ../data.sim `cat file_list`

rm file_list sim/jedit-*
