JEDIT 3.2 README

* Contents

- About jEdit
- jEdit on the Internet
- Reporting bugs
- Suggesting features
- Documentation
- Common problems
- Problems that occur with older Java versions
- Libraries
- Credits

* About jEdit

jEdit is a cross platform programmer's text editor written in Java.

jEdit is free software, and you are welcome to redistribute it under the
terms of the GNU General Public License (either version 2 or any later
version, at the user's election). See the file COPYING.txt for details.

A number of plugins are available for jEdit. Unless otherwise stated in
the plugin's documentation, each of the plugins is licensed for use and
redistribution under the terms of the GNU General Public License (either
version 2 or any later version, at the user's election).

jEdit requires Java 2 (or Java 1.1 with Swing 1.1) in order to work.

* jEdit on the Internet

The jEdit homepage, located at <http://www.jedit.org> contains the
latest version of jEdit, along with plugin downloads.

There are three mailing lists dedicated to jEdit; an announcement list
that is very low traffic, a general discussion list, and a development
discussion list. To subscribe, unsubscribe or view list archives, visit
<http://www.sourceforge.net/mail/?group_id=588>.

If you would like to discuss the BeanShell scripting language,
subscribe to one of the BeanShell mailing lists by visiting
<http://www.beanshell.org/contact.html>.

Finally, you may contact me directly by e-mailing <slava@jedit.org>.

* Reporting bugs

Before reporting a bug, please read the `Common Problems' section below.
If that doesn't answer your question, report a bug with our bug tracker;
<http://sourceforge.net/tracker/?group_id=588&atid=100588>. When writing
a bug report, please try to be as specific as possible. You should
specify your jEdit version, Java version, operating system, any relevant
output from the activity log, and an e-mail address, in case we need
further information to fix the bug.

* Suggesting features

The preferred way to suggest features is to post a message to the
jEdit-users or jEdit-devel mailing list.

* Documentation

An HTML version of the user's guide is included with jEdit; to view it,
invoke `Help->jEdit Help' from jEdit's menu bar.

A PDF version can be downloaded from <http://www.jedit.org>. It can be
viewed in a PDF viewer such as Adobe Acrobat.

* Common problems

Before reporting a problem with jEdit, please make sure it is not
actually a Java bug, or a well-known problem.

- If you get an OutOfMemoryError while editing a large file, even if
  your computer has a large amount of RAM present, increase the Java
  virtual machine heap size.

  - On Windows, run "Set jEdit Parameters" from the "jEdit" group in the
    Programs menu. Then, in the resulting dialog box, under "Command
    line options for Java executable", change the option that looks like
    so:

    -mx32m

    Replacing '32' with the desired heap size, in megabytes.

  - On Unix, edit the `jedit' shell script and change the line that
    looks like so:

    JAVA_HEAP_SIZE=32

  In both cases, replace `32' with the desired heap size, in megabytes.
  For best results, use a heap size of about 2.5 times the largest file
  size you plan to edit.

- Printing doesn't work very well, especially on Java 2. There isn't
  much I can do about this until Sun fixes several outstanding bugs in
  Java.

- Aborting I/O operations in the I/O Progress Monitor doesn't always
  work.

- Edit modes you write will not be available in the editor until you add
  an entry to the `catalog' file in the appropriate modes directory.

- International keyboards, input methods, composed keys, etc. might not
  work properly. As I do not have an international keyboard, this will
  likely remain unfixed until someone submits the necessary code.

- The Swing HTML component used by jEdit's help viewer is very buggy.
  Although the jEdit online help works around many of the bugs, it still
  renders some HTML incorrectly and runs very slowly.

- On Unix systems with X Windows, you might not be able to copy and
  paste between jEdit and other programs. This is mainly because Java
  can only access the system clipboard, and not the primary selection
  (which some programs use instead of the clipboard). The XClipboard
  plugin available from <http://plugins.jedit.org> solves part of the
  problem by allowing read-only access to the primary selection.

- Anti-aliased text might not display correctly with some Java versions.
  If you enabled anti-aliasing and are seeing problems such as text
  being drawn with the wrong font style, try using a different Java
  version or disable anti-aliasing.

- Because jEdit is written in Java, it will always be slower than a
  native application. The performance gap can be narrowed by installing
  a good virtual machine and just in time compiler.

* Problems that occur with older Java versions

- If you are having problems such as Alt-key mnemonics not working, or
  keystrokes inserting garbage into the text area, make sure you are
  running the very latest Java version for your platform. Some older
  Java versions, especially on Linux, had buggy key handling.

- If you are using Java 1.1 and get a `ClassNotFoundException:
  javax/swing/JWindow' or similar exception when starting jEdit,
  chances are you don't have Swing installed properly. Download Swing
  from <http://java.sun.com/products/jfc>. Alternatively, upgrade to
  Java 2, which doesn't require you to install Swing separately.

- The following two source files will not compile under Java 1.1:

  bsh/XThis.java
  org/gjt/sp/jedit/textarea/TextRenderer2D.java

  These files use Java 2-specific features, and are only used when
  running on Java 2. You can safely delete or ignore them if you only
  plan on running jEdit in Java 1.1.

* Libraries

jEdit depends on, and comes bundled with the following libraries:

- gnu.regexp by the Free Software Foundation. jEdit bundles gnu.regexp
  1.1.3, with the following minor change:

  - REMatch.getSubCount() method added

  The gnu.regexp homepage is <http://www.cacas.org/java/gnu/regexp/>.

- AElfred XML parser by Microstar corporation. This class library is
  released under its own, non-GPL license, which reads as follows:

  "AElfred is free for both commercial and non-commercial use and
  redistribution, provided that Microstar's copyright and disclaimer are
  retained intact.  You are free to modify AElfred for your own use and
  to redistribute AElfred with your modifications, provided that the
  modifications are clearly documented."

  The AElfred home page is <http://www.microstar.com>.

- BeanShell scripting language, by Pat Niemeyer. jEdit bundles an
  unmodified BeanShell, version 1.1a18, sans bsh.util package.

  The BeanShell homepage is located at <http://www.beanshell.org>.

- The Sun Java look and Feel icon collection. The license may be found
  in the ICONS.LICENSE.txt file.

* Credits

The following people contributed to the jEdit core:

Alwyn Barry:
	- Ada95 syntax highlighting

Andre Kaplan:
	- ASP, JavaScript, VBScript, BCEL syntax highlighting
	- Syntax token background highlighting

Artur Biesiadowski:
	- Eiffel syntax highlighting

Ben Sarsgard:
	- Improved PHP syntax highlighting

Bruno Tatibouet:
	- B formal method syntax highlighting

Dante Fabrizio:
	- VHDL syntax highlighting

Denis Lambot:
	- Selecting files from the keyboard in the file system browser

Dirk Moebius:
	- HTTP firewall plugin
	- EditorExitRequested message code
	- VariableGridLayout code

Dominic Stolerman:
	- ML syntax highlighting

Gili Tzabari:
	- OS/2 start script

Ian Maclean:
	- Ruby syntax highlighting

Jason Ginchereau:
	- Portions of QuickFile plugin are used in file system browser

Jerry McBride:
	- NetRexx syntax highlighting

John Gellene:
	- Writing macros section of user's guide
	- Writing plugins section of user's guide
	- jEditLauncher for Windows
	- A large number of sample macros
	- ... and more

Jonathan Revusky:
	- Python syntax highlighting

Juha Lindfors:
	- IDL syntax highlighting

Kristian Ovaska:
	- Intel x86 Assembly, Haskell, Pascal syntax highlighting

Lionel Fiol:
	- Omnimark syntax highlighting

Marc Rummel:
	- MIPS R2000 Assembly syntax highlighting

Matt Albrecht:
	- Inform syntax highlighting

Matthias Schneider:
	- AWK, COBOL syntax highlighting

Michael Jacobson:
	- TCL syntax highlighting

Mike Dillon:
	- XMode syntax highlighting engine
	- Gutter
	- New options dialog box
	- Faster literal search
	- ColdFusion, CSS, HTML, Java, JSP, LISP, Patch, PHP, SHTML,
	  TSQL, XML, XSL syntax highlighting
	- Plugin central (http://plugins.jedit.org) web master
	- ... and more

Nitsan Vardi:
	- VHDL syntax highlighting

Ollie Rutherfurd:
	- C# syntax highlighting
	- Improvements to XSL syntax highlighting
	- Default set of XSL abbreviations included with jEdit
	- Some commands in View->Scrolling menu
	- ... and more

Patrick Tingen:
	- Progress 4GL syntax highlighting

Peter Graves:
	- Code for preserving file permissions on Unix was taken from
	  Peter's j text editor (http://www.armedbear.org)

Ralf Engels:
	- PostScript syntax highlighting

Romain Guy:
	- POVRay syntax highlighting

Richard Beton:
	- Occam syntax highlighting

Saimon Moore:
	- Velocity syntax highlighting

Tal Davidson:
	- First implementation of syntax highlighting engine

Tom Locke:
	- Reverse search

Wojciech Stryjewski:
	- Verilog syntax highlighting

If you are not on the above list but think you should be, e-mail me.

In addition to the above people, I would like to thank the plugin
developers for their plugins, and the users for their feedback and
comments.
