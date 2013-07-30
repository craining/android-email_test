package com.example.k9testtest.imap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.example.k9testtest.common.MessagingException;
import com.example.k9testtest.imap.ImapResponseParser.ImapResponse;
import com.example.k9testtest.imap.ImapStore.ImapConnection;

public class ImapTest {

	/**
	 * ≤‚ ‘imap√¸¡Ó
	 * 
	 * @Description:
	 * @see:
	 * @since:
	 * @author: zhuanggy
	 * @date:2013-7-30
	 */
	public static void doImapCommand() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				ImapConnection connection = null;

				try {
					ImapStore imap = new ImapStore();
					connection = imap.getConnection();
					// connection.executeSimpleCommand(String.format("CREATE \"%s\"", "testabcd"));
					// connection.executeSimpleCommand(String.format("DELETE \"%s\"", "testabcd"));

					List<ImapResponse> responses = new ArrayList<ImapResponse>();
					responses = connection.executeSimpleCommand("LIST \"\" *");

					// if(responses!=null) {
					// for(ImapResponse res : responses) {
					// Log.v("" , "response:" + res.toString());
					// }
					// }

					connection.executeSimpleCommand("Select inbox");
					connection.executeSimpleCommand("Fetch 0:6 uid");

					connection.executeSimpleCommand("Fetch 0:6 body[HEADER.FIELDS (SUBJECT)]");

				} catch (MessagingException me) {
					me.printStackTrace();
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
			}
		}).start();

	}
}
