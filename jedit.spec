###
### RPM spec file for jEdit
###

### To create the RPM, put the source tarball in the RPM SOURCES
### directory, and invoke:

### rpm -ba jedit.spec --target=noarch

### You will need to have jmk, openjade and DocBook-XML 4.1.2 installed
### for this to work.

Summary: Programmer's text editor written in Java
Name: jedit
Version: 3.2pre10
Release: 1
# REMIND: bump this with each RPM
Serial: 16
Copyright: GPL
Group: Application/Editors
Source0: http://prdownloads.sourceforge.net/jedit/jedit32pre10source.tar.gz
URL: http://www.jedit.org
Vendor: Slava Pestov <slava@jedit.org>
Packager: Slava Pestov <slava@jedit.org>
ExclusiveArch: noarch

%description
jEdit is an Open Source, cross platform text editor written in Java. It
has many advanced features that make text editing easier, such as syntax
highlighting, auto indent, abbreviation expansion, registers, macros,
regular expressions, and multiple file search and replace.

jEdit requires Java 2 (or Java 1.1 with Swing 1.1) in order to work.

%prep
rm -rf jEdit
tar xvfz $RPM_SOURCE_DIR/jedit32pre10source.tar.gz

%build
cd jEdit

# Build docs
(cd doc/users-guide/ && jmk htmldocs)

# Build jedit.jar
jmk

# Build LatestVersion.jar
(cd jars/LatestVersion && jmk)

# Build LatestVersion.jar
(cd jars/Firewall && jmk)

# Create installer filelists
sh installer/mk_filelist.sh

%install
cd jEdit
java installer.Install auto /usr/share/jedit/$RPM_PACKAGE_VERSION /usr/bin

# Create documentation link according to Linux conventions
rm -f /usr/doc/jedit-$RPM_PACKAGE_VERSION
ln -sf ../share/jedit/$RPM_PACKAGE_VERSION/doc /usr/doc/jedit-$RPM_PACKAGE_VERSION

# Create shell script for starting jEdit
cat > /usr/bin/jedit <<EOF
#!/bin/sh
# Java home directory
if [ "\$JAVA_HOME" = "" ]; then
    JAVA_HOME=/usr
fi

# Java heap size, in megabytes (see doc/README.txt)
JAVA_HEAP_SIZE=32

exec \$JAVA_HOME/bin/java -mx\${JAVA_HEAP_SIZE}m \${JEDIT} -jar /usr/share/jedit/$RPM_PACKAGE_VERSION/jedit.jar \$@

EOF

chmod +x /usr/bin/jedit
%files
/usr/bin/jedit
/usr/doc/jedit-3.2pre10
/usr/share/jedit/3.2pre10/
