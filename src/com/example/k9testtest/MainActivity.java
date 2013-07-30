package com.example.k9testtest;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.example.k9testtest.smtp_k9.SmtpTest;

public class MainActivity extends Activity {

	public static Application app;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		app = getApplication();

		// ImapTest.doImapCommand();

		SmtpTest.doSmtpCommand();

	}
}
