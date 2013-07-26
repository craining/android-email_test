package com.example.k9testtest;

import java.io.IOException;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.Menu;

import com.example.k9testtest.imap.ImapStore;
import com.example.k9testtest.imap.ImapStore.ImapConnection;
import com.example.k9testtest.imap.MessagingException;

public class MainActivity extends Activity {

	
	public static Application app;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		app = getApplication();
		
		
		
		new Thread(new Runnable() {

			@Override
			public void run() {
				ImapConnection connection = null;

				try {
					ImapStore imap = new ImapStore();
					connection = imap.getConnection();
//					connection.executeSimpleCommand(String.format("CREATE \"%s\"", "testabcd"));
//					connection.executeSimpleCommand(String.format("DELETE \"%s\"", "testabcd"));
					connection.executeSimpleCommand("LIST \"\" *");
					connection.executeSimpleCommand("Select inbox");
					connection.executeSimpleCommand("Fetch 0:6 uid");
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
