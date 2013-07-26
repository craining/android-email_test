package com.example.k9testtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.Menu;

import com.example.k9testtest.imap.ImapResponseParser.ImapResponse;
import com.example.k9testtest.imap.ImapStore;
import com.example.k9testtest.imap.ImapStore.ImapConnection;
import com.example.k9testtest.imap.MessagingException;
import com.example.k9testtest.smtp.Flag;
import com.example.k9testtest.smtp.Message;
import com.example.k9testtest.smtp.Sender;

public class MainActivity extends Activity {

	public static Application app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		app = getApplication();

		 doImapCommand();

	 

	}

	private void doImapCommand() {
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

}
