package com.example.k9testtest.imap;

/**
 * Store is the access point for an email message store. It's location can be local or remote and no specific
 * protocol is defined. Store is intended to loosely model in combination the JavaMail classes
 * javax.mail.Store and javax.mail.Folder along with some additional functionality to improve performance on
 * mobile devices. Implementations of this class should focus on making as few network connections as
 * possible.
 */
public abstract class Store {

	/**
	 * A global suggestion to Store implementors on how much of the body should be returned on
	 * FetchProfile.Item.BODY_SANE requests.
	 */
	// Matching MessagingController.MAX_SMALL_MESSAGE_SIZE
	public static final int FETCH_BODY_SANE_SUGGESTED_SIZE = (50 * 1024);

	protected static final int SOCKET_CONNECT_TIMEOUT = 30000;
	protected static final int SOCKET_READ_TIMEOUT = 60000;

}
