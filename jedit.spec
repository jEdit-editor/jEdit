###
### RPM spec file for jEdit
###

### This is a hack. For it to work, you must first install jEdit using
### the regular installer, then create a 'dummy' jedit30source.tar.gz
### file in the /usr/src/redhat/SOURCES directory.

### To create the RPM, invoke:
### rpm -ba jedit.spec --target=noarch

Summary: Programmer's text editor written in Java
Name: jedit
Version: 3.0
Release: 1
# REMIND: bump this with each RPM
Serial: 1
Copyright: GPL
Group: Application/Editors
Source0: http://download.sourceforge.net/jedit/jedit30source.tar.gz
NoSource: 0
URL: http://jedit.sourceforge.net
Packager: Slava Pestov <sp@gjt.org>
Prefix: /usr

%description
jEdit is an Open Source, cross platform text editor written in Java. It
has many advanced features that make text editing easier, such as syntax
highlighting, auto indent, abbreviation expansion, registers, macros,
regular expressions, and multiple file search and replace.

jEdit requires either Java 1.1 with Swing 1.1, or Java 2 to work.

%prep
rm -f /usr/doc/jedit-3.0
ln -sf ../share/jedit-3.0/doc /usr/doc/jedit-3.0

%build

%install

%post
# Create shell script here
mkdir -p ${RPM_INSTALL_PREFIX}/bin
echo "#!/bin/sh" > ${RPM_INSTALL_PREFIX}/bin/jedit
echo 'exec java ${JEDIT} -classpath \
	"${CLASSPATH}:'${RPM_INSTALL_PREFIX}'/share/jedit-3.0/jedit.jar" \
	org.gjt.sp.jedit.jEdit $@' >> ${RPM_INSTALL_PREFIX}/bin/jedit
chmod 755 ${RPM_INSTALL_PREFIX}/bin/jedit

%postun
rm ${RPM_INSTALL_PREFIX}/bin/jedit

%files
/usr/doc/jedit-3.0
%docdir /usr/doc/jedit-3.0/
/usr/share/jedit-3.0/
