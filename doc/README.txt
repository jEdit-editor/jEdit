JEDIT README

* About jEdit

jEdit is an Open Source, cross platform text editor written in Java. It
has many advanced features that make text editing easier, such as syntax
highlighting, auto indent, abbreviation expansion, registers, macros,
regular expressions, and multiple file search and replace.

jEdit requires either Java 1.1 with Swing 1.1, or Java 2 to work.

jEdit is released under the GNU General Public License, which can be
found in the COPYING.txt file. jEdit comes with ABSOLUTELY NO WARRANTY
OF ANY KIND; see section 11 and 12 of the GPL for details.

jEdit uses gnu.regexp by the Free Software Foundation. gnu.regexp is
released under the GNU General Public License. Only the parts of
gnu.regexp used by jEdit are included - the complete package can be
found at <http://www.cacas.org/java/gnu/regexp/>.

jEdit also uses the AElfred XML parser by Microstar corporation. This
class library is released under its own, non-GPL license, which reads
as follows:

  AElfred is free for both commercial and non-commercial use and
  redistribution, provided that Microstar's copyright and disclaimer are
  retained intact.  You are free to modify AElfred for your own use and
  to redistribute AElfred with your modifications, provided that the
  modifications are clearly documented.

The complete AElfred package can be found at <http://www.microstar.com>.

* jEdit on the Internet

The jEdit homepage, located at <http://jedit.sourceforge.net> contains
the latest version of jEdit, along with plugin downloads.

There are three mailing lists dedicated to jEdit; an announcement
list that is very low traffic, a general discussion list, and a
development discussion list. To subscribe, unsubscribe or view list
archives, visit <http://www.sourceforge.net/mail/?group_id=588>.

Finally, you may contact me directly by e-mailing <sp@gjt.org>.

* Documentation

jEdit's documentation is written in SGML-DocBook, which is a generic
documentation format that can be coverted to HTML, PDF, PostScript, and
so on.

An HTML version of the documentation is included in the base jEdit
distribution; a pretty PDF version can be obtained from
<http://jedit.sourceforge.net/download.php>.

To view the HTML version, select `Help->jEdit User Guide' in jEdit, or
open `doc/users-guide/index.html' in a WWW browser such as Netscape. To
view the PDF, open jedit-<version>.pdf in a PDF viewer such as Adobe
Acrobat.

* Common Problems

Before reporting a problem with jEdit, please make sure it is not
actually a Java bug, or a well-known problem.

- Printing doesn't work very well, especially on Java 2 This is almost
  entirely Sun's fault. Their printing implementation is very buggy.

- Some Java versions, especially early Java 2 versions and some Linux
  ports, have broken keybinding handling; Alt-key mnemonics might not
  work, some keystrokes might insert garbage into the text area, etc.
  The more recent Java versions fix these problems, so upgrade if you
  experiece them.

- The AltGR key doesn't work for some people. I'm not sure if this is a
  jEdit bug, a Java bug or both. Allegedly JDK 1.1.8 and 1.2.2 fix these
  problems, but I'm not sure.

- The Swing HTML component used by jEdit's help viewer is very buggy.
  Although it has been getting better with recent Swing releases, it
  still renders some HTML incorrectly and runs very slowly.

- On Unix, file permissions are reset to the defaults on save. There is
  no easy way to fix this except with native code.

- On Unix systems with X Windows, you might not be able to copy and
  paste between jEdit and other programs. This is mainly because Java
  can only access the system clipboard, and not the primary selection
  buffer (which some programs use instead of the clipboard). The
  XClipboard plugin available from <http://jedit.standmed.com> solves
  some of the problems by allowing read-only access to the primary
  selection buffer.

- If you get a 'ClassNotFoundException: javax/swing/JWindow' or similar
  when starting jEdit, chances are you don't have Swing installed
  properly. Download Swing from <http://java.sun.com/products/jfc>.
  Alternatively, upgrade to Java 2, which doesn't require you to install
  Swing separately.

- If a newly installed edit mode doesn't work, you probably need to
  rebuild the edit mode cache with `Utilities->Reload Edit Modes'.

- Because jEdit is written in Java, it will always be slower than a
  native application. The performance gap can be narrowed by installing
  a good virtual machine and just in time compiler.

* Credits

The following people contributed large amounts of code to the jEdit core:

Andre Kaplan - Syntax token background highlighting
Jason Ginchereau - Buffer tabs code
Mike Dillon - XMode syntax highlighting engine, gutter, new options
dialog box, faster literal search, many other patches
Tal Davidson - Original syntax highlighting engine
Valery Kondakoff - Old Complete Word command

The following people contributed edit modes:

Andre Kaplan - ASP, JavaScript, VBScript
Artur Biesiadowski - Eiffel
Clancy Malcolm - Original version of PHP3
Ian Maclean - Ruby
Jonathan Revusky - Python
Juha Lindfors - IDL
Kristian Ovaska - Pascal
Matthias Schneider - AWK and COBOL
Ralf Engels - PostScript
Romain Guy - POVRay

If you are not on the above list but think you should be, e-mail me.

In addition to the above people, I would like to thank all the people
who wrote plugins, and the users for their feedback and comments.

Have fun!

Slava Pestov
<sp@gjt.org>
