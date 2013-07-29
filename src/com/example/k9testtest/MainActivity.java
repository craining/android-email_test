package com.example.k9testtest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;

import com.example.k9testtest.common.MessagingException;
import com.example.k9testtest.imap.ImapResponseParser.ImapResponse;
import com.example.k9testtest.imap.ImapStore;
import com.example.k9testtest.imap.ImapStore.ImapConnection;
import com.example.k9testtest.smtp_k9.Address;
import com.example.k9testtest.smtp_k9.MessageTemp;
import com.example.k9testtest.smtp_k9.MessageTemp.RecipientType;
import com.example.k9testtest.smtp_k9.SmtpTransport;
import com.example.k9testtest.smtp_k9.Transport;

public class MainActivity extends Activity {

	public static Application app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		app = getApplication();

//		 doImapCommand();

		 doSmtpCommand();
		
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

	
	private void doSmtpCommand() {
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				try {
					
					/**
					 * uri=smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25
					 */

						
						
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp+tls+://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25");
					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25");
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://guangyu0:wszgy222,,!:PLAIN@smtp.gmail.com:25");
					
					 MessageTemp msg = new MessageTemp();
					 Address from = new Address("devmail35@163.com");
//					 Address from = new Address("guangyu0@gmail.com");
					 Address[] froms = new Address[1];
					 froms[0] = from;
					 
//					 Address to = new Address("devmail35@163.com");
					 Address to = new Address("devmail35@163.com");
					 Address[] tos = new Address[1];
					 tos[0] = to;
					 
					 
					 msg.setFrom(froms);
					 msg.setRecipients(RecipientType.TO, tos);
					 msg.setBody("this is mail content");
					 
					 
					 
					 
					 
//					 transport.close();
//					 transport.open();
//					 
//					 transport.executeSimpleCommand("MAIL FROM: <devmail35@163.com>");
//					 transport.executeSimpleCommand("RCPT TO: <devmail35@163.com>");
						
						
					 
					 
					 
					 transport.sendMessage( msg );
					 
					 
				} catch (Exception e) {
					e.printStackTrace();
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
