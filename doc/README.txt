JEDIT 2.7 README

* About jEdit

jEdit is an Open Source, cross platform text editor written in Java. It
has many advanced features that make text editing easier, such as syntax
highlighting, auto indent, abbreviation expansion, registers, macros,
regular expressions, and multiple file search and replace.

jEdit requires either Java 1.1 with Swing 1.1, or Java 2 to work.

jEdit is released under the _GNU General Public License_, which can be
found in the COPYING.txt file. jEdit comes with ABSOLUTELY NO WARRANTY
OF ANY KIND; see section 11 and 12 of the GPL for details.

* Class Libraries

jEdit depends on, and comes bundled with the following class libraries:

- gnu.regexp by the Free Software Foundation. This is used to implement
  regular expression search and replace, among other things. gnu.regexp
  is released under the _GNU Lesser General Public License_. The version
  included with jEdit differs slightly from the latest official version.
  The original can be found at <http://www.cacas.org/java/gnu/regexp/>.

- AElfred XML parser by Microstar corporation. This is used by the
  syntax highlighting code to parse mode files. This class library is
  released under its own, non-GPL license, which reads as follows:

  "AElfred is free for both commercial and non-commercial use and
  redistribution, provided that Microstar's copyright and disclaimer are
  retained intact.  You are free to modify AElfred for your own use and
  to redistribute AElfred with your modifications, provided that the
  modifications are clearly documented."

  The complete AElfred package can be found at <http://www.microstar.com>.

* jEdit on the Internet

The jEdit homepage, located at <http://jedit.sourceforge.net> contains
the latest version of jEdit, along with plugin downloads.

There are three mailing lists dedicated to jEdit; an announcement
list that is very low traffic, a general discussion list, and a
development discussion list. To subscribe, unsubscribe or view list
archives, visit <http://www.sourceforge.net/mail/?group_id=588>.

The preferred way to report bugs is to use our bug tracker;
<http://www.sourceforge.net/bugs/?groups_id=588>. When writing a bug
report, please try to be as specific as possible. You should specify
your jEdit version, Java version, operating system, any relevant output
from the activity log, and an e-mail address, in case we need further
information to fix the bug.

The preferred way to suggest features is to first post a message to the
jEdit-users or jEdit-devel mailing list. One of the jEdit developers
will then add the feature request to our task manager if it is deemed
appropriate. A list of pending tasks may be viewed at
<http://www.sourceforge.net/pm/?group_id=588>.

Finally, you may contact me directly by e-mailing <sp@gjt.org>.

* Documentation

An HTML version of the jEdit user's guide is included in the base jEdit
distribution; a pretty PDF version can be obtained from
<http://jedit.sourceforge.net/download.php>.

To view the HTML version, select `Help->jEdit User Guide' in jEdit, or
open `doc/users-guide/index.html' in a WWW browser such as Netscape. To
view the PDF, open jedit-<version>.pdf in a PDF viewer such as Adobe
Acrobat.

* Common Problems

Before reporting a problem with jEdit, please make sure it is not
actually a Java bug, or a well-known problem.

- If a newly installed edit mode doesn't work, you probably need to
  rebuild the edit mode cache with Utilities->Reload Edit Modes.

- Printing doesn't work very well, especially on Java 2. This is almost
  entirely Sun's fault. Their printing implementation is very buggy.
  I hear printing works better with Java 2 version 1.3, though.

- Some Java versions, especially early Java 2 versions and some Linux
  ports, have broken key binding handling; Alt-key mnemonics might not
  work, some keystrokes might insert garbage into the text area, etc.
  The more recent Java versions fix these problems, so upgrade if you
  experience them.

- The AltGR key doesn't work for some people. I'm not sure if this is a
  jEdit bug, a Java bug or both. jEdit 2.6pre6 and later might fix this
  problem.

- On a related note, composed keys and input methods might not work
  either, so international characters can be hard to type. I can't do
  anything about this until someone submits code to add composed key
  support to jEdit.

- The Swing HTML component used by jEdit's help viewer is very buggy.
  Although recent releases are getting better, it still renders some
  HTML incorrectly and runs very slowly.

- On Unix, file permissions are reset to the defaults on save if backups
  are enabled. There is no easy way to fix this except with native code.
  If this really bugs you, disable backups in Utilities->Global Options.

- On Unix systems with X Windows, you might not be able to copy and
  paste between jEdit and other programs. This is mainly because Java
  can only access the system clipboard, and not the primary selection
  buffer (which some programs use instead of the clipboard). The
  XClipboard plugin available from <http://jedit.standmed.com> solves
  part of the problem by allowing read-only access to the primary
  selection buffer.

- If you are using Java 1.1 and get a `ClassNotFoundException:
  javax/swing/JWindow' or similar exception when starting jEdit,
  chances are you don't have Swing installed properly. Download Swing
  from <http://java.sun.com/products/jfc>. Alternatively, upgrade to
  Java 2, which doesn't require you to install Swing separately.

- Because jEdit is written in Java, it will always be slower than a
  native application. The performance gap can be narrowed by installing
  a good virtual machine and just in time compiler.

* Credits

The following people contributed large amounts of code to the jEdit core:

Andre Kaplan - Syntax token background highlighting
Dirk Moebius - HTTP firewall plugin
Jason Ginchereau - Portions of QuickFile plugin code are used in the
	VFS browser
Mike Dillon - XMode syntax highlighting engine, gutter, new options
	dialog box, faster literal search, many other patches
Ollie Rutherfurd - Some commands in View->Scrolling menu
Tal Davidson - Original syntax highlighting engine

The following people contributed edit modes:

Andre Kaplan - ASP, JavaScript, VBScript
Artur Biesiadowski - Eiffel
Dante Fabrizio - VHDL
Dominic Stolerman - ML
Ian Maclean - Ruby
Jerry McBride - NetRexx
Jonathan Revusky - Python
Juha Lindfors - IDL
Kristian Ovaska - Haskell, Pascal
Matthias Schneider - AWK, COBOL
Michael P Jacobson - TCL
Ollie Rutherfurd - C#
Ralf Engels - PostScript
Romain Guy - POVRay

If you are not on the above list but think you should be, e-mail me.

In addition to the above people, I would like to thank the plugin
developers for their plugins, and the users for their feedback and
comments.

Have fun!

-- Slava Pestov
<sp@gjt.org>
