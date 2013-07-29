package com.example.k9testtest.smtp_k9;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import org.apache.james.mime4j.codec.QuotedPrintableOutputStream;

import android.util.Base64;
import android.util.Log;

import com.example.k9testtest.common.AuthenticationFailedException;
import com.example.k9testtest.common.HexUtil;
import com.example.k9testtest.common.MessagingException;
import com.example.k9testtest.common.PeekableInputStream;
import com.example.k9testtest.common.TrustManagerFactory;
import com.example.k9testtest.smtp_androidmail.EOLConvertingOutputStream;
import com.example.k9testtest.smtp_k9.MessageTemp.RecipientType;

public class SmtpTransport extends TransportTemp {

	public static final int CONNECTION_SECURITY_NONE = 0;

	public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;

	public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;

	public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;

	public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

	String mHost;

	int mPort;

	String mUsername;

	String mPassword;

	String mAuthType;

	int mConnectionSecurity;

	boolean mSecure;

	Socket mSocket;

	PeekableInputStream mIn;

	OutputStream mOut;
	private boolean m8bitEncodingAllowed;

	/**
	 * smtp://user:password@server:port CONNECTION_SECURITY_NONE smtp+tls://user:password@server:port
	 * CONNECTION_SECURITY_TLS_OPTIONAL smtp+tls+://user:password@server:port CONNECTION_SECURITY_TLS_REQUIRED
	 * smtp+ssl+://user:password@server:port CONNECTION_SECURITY_SSL_REQUIRED
	 * smtp+ssl://user:password@server:port CONNECTION_SECURITY_SSL_OPTIONAL
	 * 
	 * @param _uri
	 */
	public SmtpTransport(String _uri) throws MessagingException {
		URI uri;
		try {
			uri = new URI(_uri);
		} catch (URISyntaxException use) {
			throw new MessagingException("Invalid SmtpTransport URI", use);
		}

		String scheme = uri.getScheme();
		if (scheme.equals("smtp")) {
			mConnectionSecurity = CONNECTION_SECURITY_NONE;
			mPort = 25;
		} else if (scheme.equals("smtp+tls")) {
			mConnectionSecurity = CONNECTION_SECURITY_TLS_OPTIONAL;
			mPort = 25;
		} else if (scheme.equals("smtp+tls+")) {
			mConnectionSecurity = CONNECTION_SECURITY_TLS_REQUIRED;
			mPort = 25;
		} else if (scheme.equals("smtp+ssl+")) {
			mConnectionSecurity = CONNECTION_SECURITY_SSL_REQUIRED;
			mPort = 465;
		} else if (scheme.equals("smtp+ssl")) {
			mConnectionSecurity = CONNECTION_SECURITY_SSL_OPTIONAL;
			mPort = 465;
		} else {
			throw new MessagingException("Unsupported protocol");
		}

		 mHost = uri.getHost();

		if (uri.getPort() != -1) {
			mPort = uri.getPort();
		}

		if (uri.getUserInfo() != null) {
			try {
				String[] userInfoParts = uri.getUserInfo().split(":");
				mUsername = URLDecoder.decode(userInfoParts[0], "UTF-8");
				if (userInfoParts.length > 1) {
					mPassword = URLDecoder.decode(userInfoParts[1], "UTF-8");
				}
				if (userInfoParts.length > 2) {
					mAuthType = userInfoParts[2];
				}
			} catch (UnsupportedEncodingException enc) {
				// This shouldn't happen since the encoding is hardcoded to UTF-8
				Log.e("SmtpTransport", "Couldn't urldecode username or password.", enc);
			}
		}
	}

	@Override
	public void open() throws MessagingException {
		try {
			SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);
			if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED || mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
				SSLContext sslContext = SSLContext.getInstance("TLS");
				boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
				sslContext.init(null, new TrustManager[] { TrustManagerFactory.get(mHost, secure) }, new SecureRandom());
				mSocket = sslContext.getSocketFactory().createSocket();
				mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
				mSecure = true;
			} else {
				mSocket = new Socket();
				mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
			}

			// RFC 1047
			mSocket.setSoTimeout(SOCKET_READ_TIMEOUT);

			mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
			mOut = mSocket.getOutputStream();

			// Eat the banner
			executeSimpleCommand(null);

			InetAddress localAddress = mSocket.getLocalAddress();
			String localHost = localAddress.getHostName();

			if (localHost.equals(localAddress.getHostAddress())) {
				// IP was returned
				localHost = "[" + localHost + "]";
			}

			List<String> results = executeSimpleCommand("EHLO " + localHost);

			m8bitEncodingAllowed = results.contains("8BITMIME");

			/*
			 * TODO may need to add code to fall back to HELO I switched it from using HELO on non STARTTLS
			 * connections because of AOL's mail server. It won't let you use AUTH without EHLO. We should
			 * really be paying more attention to the capabilities and only attempting auth if it's available,
			 * and warning the user if not.
			 */
			if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
				if (results.contains("STARTTLS")) {
					executeSimpleCommand("STARTTLS");

					SSLContext sslContext = SSLContext.getInstance("TLS");
					boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
					sslContext.init(null, new TrustManager[] { TrustManagerFactory.get(mHost, secure) }, new SecureRandom());
					mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort, true);
					mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
					mOut = mSocket.getOutputStream();
					mSecure = true;
					/*
					 * Now resend the EHLO. Required by RFC2487 Sec. 5.2, and more specifically, Exim.
					 */
					results = executeSimpleCommand("EHLO " + localHost);
				} else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
					throw new MessagingException("TLS not supported but required");
				}
			}

			/*
			 * result contains the results of the EHLO in concatenated form
			 */
			boolean authLoginSupported = false;
			boolean authPlainSupported = false;
			boolean authCramMD5Supported = false;
			for (String result : results) {
				if (result.matches(".*AUTH.*LOGIN.*$") == true) {
					authLoginSupported = true;
				}
				if (result.matches(".*AUTH.*PLAIN.*$") == true) {
					authPlainSupported = true;
				}
				if (result.matches(".*AUTH.*CRAM-MD5.*$") == true && mAuthType != null && mAuthType.equals("CRAM_MD5")) {
					authCramMD5Supported = true;
				}
			}

			if (mUsername != null && mUsername.length() > 0 && mPassword != null && mPassword.length() > 0) {
				if (authCramMD5Supported) {
					saslAuthCramMD5(mUsername, mPassword);
				} else if (authPlainSupported) {
					saslAuthPlain(mUsername, mPassword);
				} else if (authLoginSupported) {
					saslAuthLogin(mUsername, mPassword);
				} else {
					throw new MessagingException("No valid authentication mechanism found.");
				}
			}
		} catch (SSLException e) {
			e.printStackTrace();
			// throw new CertificateValidationException(e.getMessage(), e);
		} catch (GeneralSecurityException gse) {
			throw new MessagingException("Unable to open connection to SMTP server due to security error.", gse);
		} catch (IOException ioe) {
			throw new MessagingException("Unable to open connection to SMTP server.", ioe);
		}
	}

	@Override
	public void sendMessage(MessageTemp message) throws MessagingException {
		close();
		open();

		// if (m8bitEncodingAllowed) {
		// message.setEncoding("8bit");
		// }

		AddressTemp[] from = message.getFrom();
		boolean possibleSend = false;
		try {
			// TODO: Add BODY=8BITMIME parameter if appropriate?
			executeSimpleCommand("MAIL FROM: " + "<" + from[0].getAddress() + ">");
			Log.i("xx", "after from");
			for (AddressTemp address : message.getRecipients(RecipientType.TO)) {
				executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
			}
			for (AddressTemp address : message.getRecipients(RecipientType.CC)) {
				executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
			}
			for (AddressTemp address : message.getRecipients(RecipientType.BCC)) {
				executeSimpleCommand("RCPT TO: " + "<" + address.getAddress() + ">");
			}
			message.setRecipients(RecipientType.BCC, null);
			executeSimpleCommand("DATA");
			//
			EOLConvertingOutputStream msgOut = new EOLConvertingOutputStream(new SmtpDataStuffing(new LineWrapOutputStream(new BufferedOutputStream(mOut, 1024), 1000)));

			// message.writeTo(msgOut);//�ʼ�����

			/***** new ***/
			String encoding = "";
			if (m8bitEncodingAllowed) {
				encoding = "8bit";
			}
			writeTo(msgOut, message.getBody(), encoding);

			// We use BufferedOutputStream. So make sure to call flush() !
			msgOut.flush();

			possibleSend = true; // After the "\r\n." is attempted, we may have sent the message
			executeSimpleCommand("\r\n.");
		} catch (Exception e) {
			MessagingException me = new MessagingException("Unable to send message", e);
			me.setPermanentFailure(possibleSend);
			throw me;
		} finally {
			close();
		}

	}

	@Override
	public void close() {
		try {
			executeSimpleCommand("QUIT");
		} catch (Exception e) {

		}
		try {
			mIn.close();
		} catch (Exception e) {

		}
		try {
			mOut.close();
		} catch (Exception e) {

		}
		try {
			mSocket.close();
		} catch (Exception e) {

		}
		mIn = null;
		mOut = null;
		mSocket = null;
	}

	private String readLine() throws IOException {
		StringBuffer sb = new StringBuffer();
		int d;
		while ((d = mIn.read()) != -1) {
			if (((char) d) == '\r') {
				continue;
			} else if (((char) d) == '\n') {
				break;
			} else {
				sb.append((char) d);
			}
		}
		String ret = sb.toString();
		Log.e("SmtpTransport", "SMTP <<< " + ret);

		return ret;
	}

	private void writeLine(String s) throws IOException {
		Log.e("SmtpTransport", "SMTP >>> " + s);

		/*
		 * Note: We can use the string length to compute the buffer size since only ASCII characters are
		 * allowed in SMTP commands i.e. this string will never contain multi-byte characters.
		 */
		int len = s.length();
		byte[] data = new byte[len + 2];
		s.getBytes(0, len, data, 0);
		data[len + 0] = '\r';
		data[len + 1] = '\n';

		/*
		 * Important: Send command + CRLF using just one write() call. Using multiple calls will likely result
		 * in multiple TCP packets and some SMTP servers misbehave if CR and LF arrive in separate pakets. See
		 * issue 799.
		 */
		mOut.write(data);
		mOut.flush();
	}

	private void checkLine(String line) throws MessagingException {
		if (line.length() < 1) {
			throw new MessagingException("SMTP response is 0 length");
		}
		char c = line.charAt(0);
		if ((c == '4') || (c == '5')) {
			throw new MessagingException(line);
		}
	}

	public List<String> executeSimpleCommand(String command) throws IOException, MessagingException {

		List<String> results = new ArrayList<String>();
		if (command != null) {
			writeLine(command);
		}

		boolean cont = false;
		do {
			String line = readLine();
			checkLine(line);
			if (line.length() > 4) {
				results.add(line.substring(4));
				if (line.charAt(3) == '-') {
					cont = true;
				} else {
					cont = false;
				}
			}
		} while (cont);
		return results;

	}

	// C: AUTH LOGIN
	// S: 334 VXNlcm5hbWU6
	// C: d2VsZG9u
	// S: 334 UGFzc3dvcmQ6
	// C: dzNsZDBu
	// S: 235 2.0.0 OK Authenticated
	//
	// Lines 2-5 of the conversation contain base64-encoded information. The same conversation, with base64
	// strings decoded, reads:
	//
	//
	// C: AUTH LOGIN
	// S: 334 Username:
	// C: weldon
	// S: 334 Password:
	// C: w3ld0n
	// S: 235 2.0.0 OK Authenticated

	private void saslAuthLogin(String username, String password) throws MessagingException, AuthenticationFailedException, IOException {
		try {
			executeSimpleCommand("AUTH LOGIN");
			executeSimpleCommand(new String(Base64.encode(username.getBytes(), Base64.DEFAULT)));
			executeSimpleCommand(new String(Base64.encode(password.getBytes(), Base64.DEFAULT)));
		} catch (MessagingException me) {
			if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3') {
				throw new AuthenticationFailedException("AUTH LOGIN failed (" + me.getMessage() + ")");
			}
			throw me;
		}
	}

	private void saslAuthPlain(String username, String password) throws MessagingException, AuthenticationFailedException, IOException {
		byte[] data = ("\000" + username + "\000" + password).getBytes();
		data = Base64.encode(data, Base64.DEFAULT);
		try {
			executeSimpleCommand("AUTH PLAIN " + new String(data));
		} catch (MessagingException me) {
			if (me.getMessage().length() > 1 && me.getMessage().charAt(1) == '3') {
				throw new AuthenticationFailedException("AUTH PLAIN failed (" + me.getMessage() + ")");
			}
			throw me;
		}
	}

	private void saslAuthCramMD5(String username, String password) throws MessagingException, AuthenticationFailedException, IOException {
		List<String> respList = executeSimpleCommand("AUTH CRAM-MD5");
		if (respList.size() != 1)
			throw new AuthenticationFailedException("Unable to negotiate CRAM-MD5");
		String b64Nonce = respList.get(0);
		byte[] nonce = Base64.decode(b64Nonce.getBytes("US-ASCII"), Base64.DEFAULT);
		byte[] ipad = new byte[64];
		byte[] opad = new byte[64];
		byte[] secretBytes = password.getBytes("US-ASCII");
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException nsae) {
			throw new AuthenticationFailedException("MD5 Not Available.");
		}
		if (secretBytes.length > 64) {
			secretBytes = md.digest(secretBytes);
		}
		System.arraycopy(secretBytes, 0, ipad, 0, secretBytes.length);
		System.arraycopy(secretBytes, 0, opad, 0, secretBytes.length);
		for (int i = 0; i < ipad.length; i++)
			ipad[i] ^= 0x36;
		for (int i = 0; i < opad.length; i++)
			opad[i] ^= 0x5c;
		md.update(ipad);
		byte[] firstPass = md.digest(nonce);
		md.update(opad);
		byte[] result = md.digest(firstPass);
		String plainCRAM = username + " " + new String(HexUtil.byte2hex(result));
		byte[] b64CRAM = Base64.encode(plainCRAM.getBytes("US-ASCII"), Base64.DEFAULT);
		String b64CRAMString = new String(b64CRAM, "US-ASCII");
		try {
			executeSimpleCommand(b64CRAMString);
		} catch (MessagingException me) {
			throw new AuthenticationFailedException("Unable to negotiate MD5 CRAM");
		}
	}

	/********************/

	private void writeTo(OutputStream out, String body, String encoding) throws IOException, MessagingException {
		if (body != null) {
			byte[] bytes = body.getBytes("UTF-8");
			if ("8bit".equals(encoding)) {
				out.write(bytes);
			} else {
				QuotedPrintableOutputStream qp = new QuotedPrintableOutputStream(out, false);
				qp.write(bytes);
				qp.flush();
			}
		}
	}

}