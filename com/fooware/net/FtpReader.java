/*************************************************************************
* Copyright (C) 1998, Chris Cheetham, fooware                            *
* Distributed under the GNU General Public License                       *
*   http://www.fsf.org/copyleft/gpl.html                                 *
*************************************************************************/

package com.fooware.net;

import java.io.FilterReader;
import java.io.Reader;

import java.io.IOException;

/**
* This is an object that can be used as a java.io.Reader for the
* reading of data from an FTP server.  You do not construct one
* explicitly, but via an FtpClient:<BR>
* <CODE><PRE>
* FtpClient ftp = new FtpClient();
* ftp.connect(someServer);
* ftp.userName(myName);
* ftp.password(myPassword);
* ftp.dataPort();
* <B>FtpReader in = ftp.list();
* while (in.ready())
*   System.out.println(in.readLine());
* in.close();</B>
* </PRE></CODE>
* Note: unlike other Readers, it is important to explicitly close this
* Reader when through.  This signals the FtpClient to get a response
* from the FTP server.
* @author <A HREF="mailto:cheetham@fooware.com">Chris Cheetham</A>
* @version $Revision$
**/
public class FtpReader extends FilterReader {

    //
    // constructors
    //

    /**
    * Contruct an FtpReader for the specified FtpClient.
    **/
    FtpReader(Reader in, FtpClient client) throws IOException {
        super(in);
        this.client = client;
    }

    /**
    * Close the underlying Reader and signal the FtpClient that
    * Reader processing has completed.
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

