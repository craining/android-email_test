
package com.example.k9testtest.smtp_k9;

import android.util.Log;

import com.example.k9testtest.common.MessagingException;

public abstract class Transport
{
    protected static final int SOCKET_CONNECT_TIMEOUT = 10000;

    // RFC 1047
    protected static final int SOCKET_READ_TIMEOUT = 300000;

//    public synchronized static Transport getInstance(Account account) throws MessagingException
//    {
//        String uri = account.getTransportUri();
//        if (uri.startsWith("smtp"))
//        {
//            return new SmtpTransport(uri);
//        }
//        else if (uri.startsWith("webdav"))
//        {
//            return new WebDavTransport(account);
//        }
//        else
//        {
//            throw new MessagingException("Unable to locate an applicable Transport for " + uri);
//        }
//    }

    
    public synchronized static Transport getInstance(String uri) throws MessagingException
    {
    	Log.e("", "uri=" + uri);
//        String uri = account.getTransportUri();
        if (uri.startsWith("smtp"))
        {
            return new SmtpTransport(uri);
        }
//        else if (uri.startsWith("webdav"))
//        {
//            return new WebDavTransport(account);
//        }
//        else
//        {
//            throw new MessagingException("Unable to locate an applicable Transport for " + uri);
//        }
		return null;
    }

    
    
    public abstract void open() throws MessagingException;

    public abstract void sendMessage(MessageTemp message) throws MessagingException;

    public abstract void close() throws MessagingException;
}
