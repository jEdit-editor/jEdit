package org.gjt.sp.jedit.syntax;

import java.util.Hashtable;
import org.gjt.sp.jedit.*;

// backwards compatibility will go away in pre2.

public class SyntaxDocument extends Buffer
{
	public SyntaxDocument(View view, String path, boolean readOnly,
		boolean newFile, boolean temp, Hashtable props)
	{
		super(view,path,readOnly,newFile,temp,props);
	}
}
