/*************************************************************************
* Copyright (C) 1998, Chris Cheetham, fooware                            *
* Distributed under the GNU General Public License                       *
*   http://www.fsf.org/copyleft/gpl.html                                 *
*************************************************************************/

package com.fooware.net;

import java.io.FilterWriter;
import java.io.Writer;

import java.io.IOException;

/**
* This is an object that can be used as a java.io.Writer for the
* writing of data to an FTP server.  You do not construct one
* explicitly, but via an FtpClient:<BR>
* <CODE><PRE>
* FtpClient ftp = new FtpClient();
* ftp.connect(someServer);
* ftp.userName(myName);
* ftp.password(myPassword);
* ftp.dataPort();
* <B>FtpWriter out = ftp.store(targetFile);
* while (someReader.ready())
*   out.write(someReader.read());
* in.close();</B>
* </PRE></CODE>
* Note: unlike other Writers, it is important to explicitly close this
* Writer when through.  This signals the FtpClient to get a response
* from the FTP server.
* @author <A HREF="mailto:cheetham@fooware.com">Chris Cheetham</A>
* @version $Revision$
**/
public class FtpWriter extends FilterWriter {

    //
    // constructors
    //

    /**
     * Contruct an FtpWriter for the specified FtpClient.
     **/
    FtpWriter(Writer out, FtpClient client) throws IOException {
        super(out);
        this.client = client;
    }

    /**
     * Close the underlying Writer and signal the FtpClient that
     * Writer processing has completed.
     **/
    public void close() throws IOException {
        super.close();
        client.closeTransferSocket();
    }

    //
    // member variables
    //

    private FtpClient client;

}

