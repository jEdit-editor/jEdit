###
### RPM spec file for jEdit
###

### This is a hack. For it to work, you must first install jEdit using
### the regular installer, then create a 'dummy' jedit31source.tar.gz
### file in the /usr/src/redhat/SOURCES directory.

### To create the RPM, invoke:
### rpm -ba jedit.spec --target=noarch

Summary: Programmer's text editor written in Java
Name: jedit
Version: 3.1
Release: 1
# REMIND: bump this with each RPM
Serial: 11
Copyright: GPL
Group: Application/Editors
Source0: http://download.sourceforge.net/jedit/jedit31source.tar.gz
NoSource: 0
URL: http://www.jedit.org
Packager: Slava Pestov <slava@jedit.org>

%description
jEdit is an Open Source, cross platform text editor written in Java. It
has many advanced features that make text editing easier, such as syntax
highlighting, auto indent, abbreviation expansion, registers, macros,
regular expressions, and multiple file search and replace.

jEdit requires Java 2 (or Java 1.1 with Swing 1.1) in order to work.

%prep
rm -f /usr/doc/jedit-3.1
ln -sf ../share/jedit/3.1/doc /usr/doc/jedit-3.1

%build

%install

%files
/usr/bin/jedit
/usr/doc/jedit-3.1
%docdir /usr/doc/jedit-3.1/
/usr/share/jedit/3.1/
