package com.example.k9testtest.imap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Base64;
import android.util.Log;

import com.example.k9testtest.MainActivity;
import com.example.k9testtest.common.AuthenticationFailedException;
import com.example.k9testtest.common.HexUtil;
import com.example.k9testtest.common.MessagingException;
import com.example.k9testtest.common.PeekableInputStream;
import com.example.k9testtest.common.TrustManagerFactory;
import com.example.k9testtest.imap.ImapResponseParser.ImapList;
import com.example.k9testtest.imap.ImapResponseParser.ImapResponse;

public class ImapStore extends Store {

	class ImapException extends MessagingException {

		String mAlertText;

		public ImapException(String message, String alertText, Throwable throwable) {
			super(message, throwable);
			this.mAlertText = alertText;
		}

		public ImapException(String message, String alertText) {
			super(message);
			this.mAlertText = alertText;
		}

		public String getAlertText() {
			return mAlertText;
		}

		public void setAlertText(String alertText) {
			mAlertText = alertText;
		}
	}

	private interface UntaggedHandler {

		void handleAsyncUntaggedResponse(ImapResponse respose);
	}

	public static final int CONNECTION_SECURITY_NONE = 0;
	public static final int CONNECTION_SECURITY_TLS_OPTIONAL = 1;
	public static final int CONNECTION_SECURITY_TLS_REQUIRED = 2;
	public static final int CONNECTION_SECURITY_SSL_REQUIRED = 3;
	public static final int CONNECTION_SECURITY_SSL_OPTIONAL = 4;

	private enum AuthType {
		PLAIN, CRAM_MD5
	};

	private static final String CAPABILITY_IDLE = "IDLE";
	private static final String COMMAND_IDLE = "IDLE";
	private static final String CAPABILITY_NAMESPACE = "NAMESPACE";
	private static final String COMMAND_NAMESPACE = "NAMESPACE";

	private static final String CAPABILITY_CAPABILITY = "CAPABILITY";
	private static final String COMMAND_CAPABILITY = "CAPABILITY";

	private static final String CAPABILITY_COMPRESS_DEFLATE = "COMPRESS=DEFLATE";
	private static final String COMMAND_COMPRESS_DEFLATE = "COMPRESS DEFLATE";

	//test
	private String mHost = "imap.163.com";
	private int mPort = 143;
	private String mUsername = "devmail35@163.com";
	private String mPassword = "QWERTYUIOP";

	private int mConnectionSecurity = CONNECTION_SECURITY_NONE;//
	private AuthType mAuthType = AuthType.PLAIN;
	private volatile String mPathPrefix;
	private volatile String mCombinedPrefix = null;
	private volatile String mPathDelimeter = null;

	public class ImapConnection {

		private Socket mSocket;
		private PeekableInputStream mIn;
		private OutputStream mOut;
		private ImapResponseParser mParser;
		private int mNextCommandTag;
		protected Set<String> capabilities = new HashSet<String>();

		private String getLogId() {
			return "conn" + hashCode();
		}

		private List<ImapResponse> receiveCapabilities(List<ImapResponse> responses) {
			for (ImapResponse response : responses) {
				ImapList capabilityList = null;
				if (response.size() > 0 && ImapResponseParser.equalsIgnoreCase(response.get(0), "OK")) {
					for (Object thisPart : response) {
						if (thisPart instanceof ImapList) {
							ImapList thisList = (ImapList) thisPart;
							if (ImapResponseParser.equalsIgnoreCase(thisList.get(0), CAPABILITY_CAPABILITY)) {
								capabilityList = thisList;
								break;
							}
						}
					}
				} else if (response.mTag == null) {
					capabilityList = response;
				}

				if (capabilityList != null) {
					if (capabilityList.size() > 0 && ImapResponseParser.equalsIgnoreCase(capabilityList.get(0), CAPABILITY_CAPABILITY)) {
						Log.d("ImapStore-ImapConnection", "Saving " + capabilityList.size() + " capabilities for " + getLogId());
						for (Object capability : capabilityList) {
							if (capability instanceof String) {
								// if (K9.DEBUG)
								// {
								// Log.v("ImapStore-ImapConnection", "Saving capability '" + capability +
								// "' for " +
								// getLogId());
								// }
								capabilities.add(((String) capability).toUpperCase());
							}
						}

					}
				}
			}
			return responses;
		}

		public void open() throws IOException, MessagingException {
			if (isOpen()) {
				return;
			}

			boolean authSuccess = false;

			mNextCommandTag = 1;
			try {
				Security.setProperty("networkaddress.cache.ttl", "0");
				// java.security.Security.setProperty("networkaddress.cache.ttl", "5");
				// java.security.Security.setProperty("networkaddress.cache.negative.ttl","5");

			} catch (Exception e) {
				Log.w("ImapStore-ImapConnection", "Could not set DNS ttl to 0 for " + getLogId(), e);
			}

			try {

				SocketAddress socketAddress = new InetSocketAddress(mHost, mPort);

				Log.i("ImapStore-ImapConnection", "Connection " + getLogId() + " connecting to " + mHost + " @ IP addr " + socketAddress + "  mConnectionSecurity=" + mConnectionSecurity);

				if (mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED || mConnectionSecurity == CONNECTION_SECURITY_SSL_OPTIONAL) {
					SSLContext sslContext = SSLContext.getInstance("TLS");
					final boolean secure = mConnectionSecurity == CONNECTION_SECURITY_SSL_REQUIRED;
					sslContext.init(null, new TrustManager[] { TrustManagerFactory.get(mHost, secure) }, new SecureRandom());
					mSocket = sslContext.getSocketFactory().createSocket();
					mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
				} else {
					mSocket = new Socket();
					mSocket.connect(socketAddress, SOCKET_CONNECT_TIMEOUT);
				}

				setReadTimeout(Store.SOCKET_READ_TIMEOUT);

				mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
				mParser = new ImapResponseParser(mIn);
				mOut = mSocket.getOutputStream();

				capabilities.clear();
				ImapResponse nullResponse = mParser.readResponse();
				Log.v("ImapStore-ImapConnection", getLogId() + "<<<" + nullResponse);

				List<ImapResponse> nullResponses = new LinkedList<ImapResponse>();
				nullResponses.add(nullResponse);
				receiveCapabilities(nullResponses);

				if (hasCapability(CAPABILITY_CAPABILITY) == false) {
					Log.i("ImapStore-ImapConnection", "Did not get capabilities in banner, requesting CAPABILITY for " + getLogId());
					List<ImapResponse> responses = receiveCapabilities(executeSimpleCommand(COMMAND_CAPABILITY));
					if (responses.size() != 2) {
						throw new MessagingException("Invalid CAPABILITY response received");
					}
				}

				if (mConnectionSecurity == CONNECTION_SECURITY_TLS_OPTIONAL || mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {

					if (hasCapability("STARTTLS")) {
						// STARTTLS
						executeSimpleCommand("STARTTLS");

						SSLContext sslContext = SSLContext.getInstance("TLS");
						boolean secure = mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED;
						sslContext.init(null, new TrustManager[] { TrustManagerFactory.get(mHost, secure) }, new SecureRandom());
						mSocket = sslContext.getSocketFactory().createSocket(mSocket, mHost, mPort, true);
						mSocket.setSoTimeout(Store.SOCKET_READ_TIMEOUT);
						mIn = new PeekableInputStream(new BufferedInputStream(mSocket.getInputStream(), 1024));
						mParser = new ImapResponseParser(mIn);
						mOut = mSocket.getOutputStream();
					} else if (mConnectionSecurity == CONNECTION_SECURITY_TLS_REQUIRED) {
						throw new MessagingException("TLS not supported but required");
					}
				}

				mOut = new BufferedOutputStream(mOut, 1024);

				try {
					if (mHost.endsWith("yahoo.com")) {
						Log.v("ImapStore-ImapConnection", "Found Yahoo! account.  Sending proprietary commands.");
						executeSimpleCommand("ID (\"GUID\" \"1\")");
					}
					if (mAuthType == AuthType.CRAM_MD5) {
						authCramMD5();
						// The authCramMD5 method called on the previous line does not allow for handling
						// updated capabilities
						// sent by the server. So, to make sure we update to the post-authentication
						// capability list
						// we fetch the capabilities here.
						Log.i("ImapStore-ImapConnection", "Updating capabilities after CRAM-MD5 authentication for " + getLogId());
						List<ImapResponse> responses = receiveCapabilities(executeSimpleCommand(COMMAND_CAPABILITY));
						if (responses.size() != 2) {
							throw new MessagingException("Invalid CAPABILITY response received");
						}

					} else if (mAuthType == AuthType.PLAIN) {
						receiveCapabilities(executeSimpleCommand("LOGIN \"" + escapeString(mUsername) + "\" \"" + escapeString(mPassword) + "\"", true));
					}
					authSuccess = true;
				} catch (ImapException ie) {
					throw new AuthenticationFailedException(ie.getAlertText(), ie);

				} catch (MessagingException me) {
					throw new AuthenticationFailedException(null, me);
				}
				Log.d("ImapStore-ImapConnection", CAPABILITY_COMPRESS_DEFLATE + " = " + hasCapability(CAPABILITY_COMPRESS_DEFLATE));
				if (hasCapability(CAPABILITY_COMPRESS_DEFLATE)) {
					ConnectivityManager connectivityManager = (ConnectivityManager) MainActivity.app.getSystemService(Context.CONNECTIVITY_SERVICE);

					// ÊÇ·ñÊÇÑ¹ËõµÄ

					// boolean useCompression = true;
					//
					// NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
					// if (netInfo != null) {
					// int type = netInfo.getType();
					// Log.d("ImapStore-ImapConnection", "On network type " + type);
					// useCompression = mAccount.useCompression(type);
					//
					// }
					// Log.d("ImapStore-ImapConnection", "useCompression " + useCompression);
					// if (useCompression) {
					// try {
					// executeSimpleCommand(COMMAND_COMPRESS_DEFLATE);
					// ZInputStream zInputStream = new ZInputStream(mSocket.getInputStream(), true);
					// zInputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);
					// mIn = new PeekableInputStream(new BufferedInputStream(zInputStream, 1024));
					// mParser = new ImapResponseParser(mIn);
					// ZOutputStream zOutputStream = new ZOutputStream(mSocket.getOutputStream(),
					// JZlib.Z_BEST_SPEED, true);
					// mOut = new BufferedOutputStream(zOutputStream, 1024);
					// zOutputStream.setFlushMode(JZlib.Z_PARTIAL_FLUSH);
					// Log.i("ImapStore-ImapConnection", "Compression enabled for " + getLogId());
					// } catch (Exception e) {
					// Log.e("ImapStore-ImapConnection", "Unable to negotiate compression", e);
					// }
					// }
				}

				Log.d("ImapStore-ImapConnection", "NAMESPACE = " + hasCapability(CAPABILITY_NAMESPACE) + ", mPathPrefix = " + mPathPrefix);

				if (mPathPrefix == null) {
					if (hasCapability(CAPABILITY_NAMESPACE)) {
						Log.i("ImapStore-ImapConnection", "mPathPrefix is unset and server has NAMESPACE capability");
						List<ImapResponse> namespaceResponses = executeSimpleCommand(COMMAND_NAMESPACE);
						for (ImapResponse response : namespaceResponses) {
							if (ImapResponseParser.equalsIgnoreCase(response.get(0), COMMAND_NAMESPACE)) {
								Log.d("ImapStore-ImapConnection", "Got NAMESPACE response " + response + " on " + getLogId());

								Object personalNamespaces = response.get(1);
								if (personalNamespaces != null && personalNamespaces instanceof ImapList) {
									Log.d("ImapStore-ImapConnection", "Got personal namespaces: " + personalNamespaces);
									ImapList bracketed = (ImapList) personalNamespaces;
									Object firstNamespace = bracketed.get(0);
									if (firstNamespace != null && firstNamespace instanceof ImapList) {
										Log.d("ImapStore-ImapConnection", "Got first personal namespaces: " + firstNamespace);
										bracketed = (ImapList) firstNamespace;
										mPathPrefix = bracketed.getString(0);
										mPathDelimeter = bracketed.getString(1);
										mCombinedPrefix = null;
										Log.d("ImapStore-ImapConnection", "Got path '" + mPathPrefix + "' and separator '" + mPathDelimeter + "'");
									}
								}
							}
						}
					} else {
						Log.i("ImapStore-ImapConnection", "mPathPrefix is unset but server does not have NAMESPACE capability");
						mPathPrefix = "";
					}
				}
				if (mPathDelimeter == null) {
					try {
						List<ImapResponse> nameResponses = executeSimpleCommand(String.format("LIST \"\" \"\""));
						for (ImapResponse response : nameResponses) {
							if (ImapResponseParser.equalsIgnoreCase(response.get(0), "LIST")) {
								mPathDelimeter = response.getString(2);
								mCombinedPrefix = null;
								Log.d("ImapStore-ImapConnection", "Got path delimeter '" + mPathDelimeter + "' for " + getLogId());
							}
						}
					} catch (Exception e) {
						Log.e("ImapStore-ImapConnection", "Unable to get path delimeter using LIST", e);
					}
				}

			} catch (SSLException e) {
				e.printStackTrace();
				// throw new CertificateValidationException(e.getMessage(), e);
			} catch (GeneralSecurityException gse) {
				throw new MessagingException("Unable to open connection to IMAP server due to security error.", gse);
			} catch (ConnectException ce) {
				String ceMess = ce.getMessage();
				String[] tokens = ceMess.split("-");
				if (tokens != null && tokens.length > 1 && tokens[1] != null) {
					Log.e("ImapStore-ImapConnection", "Stripping host/port from ConnectionException for " + getLogId(), ce);
					throw new ConnectException(tokens[1].trim());
				} else {
					throw ce;
				}
			} finally {
				if (authSuccess == false) {
					Log.e("ImapStore-ImapConnection", "Failed to login, closing connection for " + getLogId());
					close();
				}
			}
		}

		protected void authCramMD5() throws AuthenticationFailedException, MessagingException {
			try {
				String tag = sendCommand("AUTHENTICATE CRAM-MD5", false);
				byte[] buf = new byte[1024];
				int b64NonceLen = 0;
				for (int i = 0; i < buf.length; i++) {
					buf[i] = (byte) mIn.read();
					if (buf[i] == 0x0a) {
						b64NonceLen = i;
						break;
					}
				}
				if (b64NonceLen == 0) {
					throw new AuthenticationFailedException("Error negotiating CRAM-MD5: nonce too long.");
				}
				byte[] b64NonceTrim = new byte[b64NonceLen - 2];
				System.arraycopy(buf, 1, b64NonceTrim, 0, b64NonceLen - 2);

				// byte[] nonce = Base64.decodeBase64(b64NonceTrim);
				byte[] nonce = Base64.encode(b64NonceTrim, Base64.DEFAULT);
				Log.d("ImapStore-ImapConnection", "Got nonce: " + new String(b64NonceTrim, "US-ASCII"));
				Log.d("ImapStore-ImapConnection", "Plaintext nonce: " + new String(nonce, "US-ASCII"));

				byte[] ipad = new byte[64];
				byte[] opad = new byte[64];
				byte[] secretBytes = mPassword.getBytes("US-ASCII");
				MessageDigest md = MessageDigest.getInstance("MD5");
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
				String plainCRAM = mUsername + " " + new String(HexUtil.byte2hex(result));
				// byte[] b64CRAM = Base64.encodeBase64(plainCRAM.getBytes("US-ASCII"));
				byte[] b64CRAM = Base64.encode(plainCRAM.getBytes("US-ASCII"), Base64.DEFAULT);
				Log.d("ImapStore-ImapConnection", "Username == " + mUsername);
				Log.d("ImapStore-ImapConnection", "plainCRAM: " + plainCRAM);
				Log.d("ImapStore-ImapConnection", "b64CRAM: " + new String(b64CRAM, "US-ASCII"));

				mOut.write(b64CRAM);
				mOut.write(new byte[] { 0x0d, 0x0a });
				mOut.flush();
				int respLen = 0;
				for (int i = 0; i < buf.length; i++) {
					buf[i] = (byte) mIn.read();
					if (buf[i] == 0x0a) {
						respLen = i;
						break;
					}
				}
				String toMatch = tag + " OK";
				String respStr = new String(buf, 0, respLen);
				if (!respStr.startsWith(toMatch)) {
					throw new AuthenticationFailedException("CRAM-MD5 error: " + respStr);
				}
			} catch (IOException ioe) {
				throw new AuthenticationFailedException("CRAM-MD5 Auth Failed.");
			} catch (NoSuchAlgorithmException nsae) {
				throw new AuthenticationFailedException("MD5 Not Available.");
			}
		}

		protected void setReadTimeout(int millis) throws SocketException {
			mSocket.setSoTimeout(millis);
		}

		protected boolean isIdleCapable() {
			Log.v("ImapStore-ImapConnection", "Connection " + getLogId() + " has " + capabilities.size() + " capabilities");

			return capabilities.contains(CAPABILITY_IDLE);
		}

		protected boolean hasCapability(String capability) {
			return capabilities.contains(capability.toUpperCase());
		}

		private boolean isOpen() {
			return (mIn != null && mOut != null && mSocket != null && mSocket.isConnected() && !mSocket.isClosed());
		}

		private void close() {
			// if (isOpen()) {
			// try {
			// executeSimpleCommand("LOGOUT");
			// } catch (Exception e) {
			//
			// }
			// }
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

		private ImapResponse readResponse() throws IOException, MessagingException {
			return readResponse(null);
		}

		private ImapResponse readResponse(ImapResponseParser.IImapResponseCallback callback) throws IOException, MessagingException {
			try {
				ImapResponse response = mParser.readResponse(callback);
				Log.v("ImapStore-ImapConnection", getLogId() + "<<<" + response);

				return response;
			} catch (IOException ioe) {
				close();
				throw ioe;
			}
		}

		private String escapeString(String in) {
			if (in == null) {
				return null;
			}
			String out = in.replaceAll("\\\\", "\\\\\\\\");
			out = out.replaceAll("\"", "\\\\\"");
			return out;
		}

		private void sendContinuation(String continuation) throws IOException {
			mOut.write(continuation.getBytes());
			mOut.write('\r');
			mOut.write('\n');
			mOut.flush();

			Log.v("ImapStore-ImapConnection", getLogId() + ">>> " + continuation);

		}

		public String sendCommand(String command, boolean sensitive) throws MessagingException, IOException {
			try {
				open();
				String tag = Integer.toString(mNextCommandTag++);
				String commandToSend = tag + " " + command;
				mOut.write(commandToSend.getBytes());
				mOut.write('\r');
				mOut.write('\n');
				mOut.flush();

				if (sensitive) {
					Log.v("ImapStore-ImapConnection", getLogId() + ">>> " + "[Command Hidden, Enable Sensitive Debug Logging To Show]");
				} else {
					Log.v("ImapStore-ImapConnection", getLogId() + ">>> " + commandToSend);
				}

				return tag;
			} catch (IOException ioe) {
				close();
				throw ioe;
			} catch (ImapException ie) {
				close();
				throw ie;
			} catch (MessagingException me) {
				close();
				throw me;
			}
		}

		public List<ImapResponse> executeSimpleCommand(String command) throws IOException, ImapException, MessagingException {
			return executeSimpleCommand(command, false);
		}

		public List<ImapResponse> executeSimpleCommand(String command, boolean sensitive) throws IOException, ImapException, MessagingException {
			return executeSimpleCommand(command, sensitive, null);
		}

		private List<ImapResponse> executeSimpleCommand(String command, boolean sensitive, UntaggedHandler untaggedHandler) throws IOException, ImapException, MessagingException {
			String commandToLog = command;
			if (sensitive) {
				commandToLog = "*sensitive*";
			}

			Log.v("ImapStore-ImapConnection", "Sending IMAP command " + commandToLog + " on connection " + getLogId());

			String tag = sendCommand(command, sensitive);
			// Log.v("ImapStore-ImapConnection", "Sent IMAP command " + commandToLog + " with tag " + tag +
			// " for " + getLogId());

			ArrayList<ImapResponse> responses = new ArrayList<ImapResponse>();
			ImapResponse response;
			do {
				response = mParser.readResponse();
				// Log.v("ImapStore-ImapConnection", getLogId() + "<<<" + response);

				if (response.mTag != null && response.mTag.equalsIgnoreCase(tag) == false) {
					Log.w("ImapStore-ImapConnection", "After sending tag " + tag + ", got tag response from previous command " + response + " for " + getLogId());
					Iterator<ImapResponse> iter = responses.iterator();
					while (iter.hasNext()) {
						ImapResponse delResponse = iter.next();
						if (delResponse.mTag != null || delResponse.size() < 2 || (ImapResponseParser.equalsIgnoreCase(delResponse.get(1), "EXISTS") == false && ImapResponseParser.equalsIgnoreCase(delResponse.get(1), "EXPUNGE") == false)) {
							iter.remove();
						}
					}
					response.mTag = null;
					continue;
				}
				if (untaggedHandler != null) {
					untaggedHandler.handleAsyncUntaggedResponse(response);
				}
				responses.add(response);
			} while (response.mTag == null);
			if (response.size() < 1 || !ImapResponseParser.equalsIgnoreCase(response.get(0), "OK")) {
				throw new ImapException("Command: " + commandToLog + "; response: " + response.toString(), response.getAlertText());
			}
			return responses;
		}
	}

	public ImapConnection getConnection() {
		ImapConnection connection = null;
		connection = new ImapConnection();
		return connection;
	}

}
