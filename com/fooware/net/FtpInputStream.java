/*************************************************************************
* Copyright (C) 1998, Chris Cheetham, fooware                            *
* Distributed under the GNU General Public License                       *
*   http://www.fsf.org/copyleft/gpl.html                                 *
*************************************************************************/

package com.fooware.net;

import java.io.FilterInputStream;
import java.io.InputStream;

import java.io.IOException;

/**
* @author <A HREF="mailto:cheetham@fooware.com">Chris Cheetham</A>
* @version $Revision$
**/
public class FtpInputStream extends FilterInputStream {

    //
    // constructors
    //

    /**
    * Contruct an FtpInputStream for the specified FtpClient.
    **/
    FtpInputStream(InputStream istr, FtpClient client) throws IOException {
        super(istr);
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

