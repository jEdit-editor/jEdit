/*************************************************************************
* Copyright (C) 1998, Chris Cheetham, fooware                            *
* Distributed under the GNU General Public License                       *
*   http://www.fsf.org/copyleft/gpl.html                                 *
*************************************************************************/

package com.fooware.net;

import java.io.FilterOutputStream;
import java.io.OutputStream;

import java.io.IOException;

/**
* @author <A HREF="mailto:cheetham@fooware.com">Chris Cheetham</A>
* @version $Revision$
**/
public class FtpOutputStream extends FilterOutputStream {

    //
    // constructors
    //

    /**
     * Contruct an FtpOutputStream for the specified FtpClient.
     **/
    FtpOutputStream(OutputStream ostr, FtpClient client) throws IOException {
        super(ostr);
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

