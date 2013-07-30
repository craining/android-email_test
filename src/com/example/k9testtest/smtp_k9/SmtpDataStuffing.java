package com.example.k9testtest.smtp_k9;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.util.Log;

public class SmtpDataStuffing extends FilterOutputStream {

	private static final int STATE_NORMAL = 0;
	private static final int STATE_CR = 1;
	private static final int STATE_CRLF = 2;

	private int state = STATE_NORMAL;

	public SmtpDataStuffing(OutputStream out) {
		super(out);
	}

	@Override
	public void write(int oneByte) throws IOException {
		if (oneByte == '\r') {
			state = STATE_CR;
			Log.v("SmtpDataStuffing", "oneByte= '\r'" );
		} else if ((state == STATE_CR) && (oneByte == '\n')) {
			state = STATE_CRLF;
			Log.v("SmtpDataStuffing", "oneByte= '\n'" );
		} else if ((state == STATE_CRLF) && (oneByte == '.')) {
			Log.v("SmtpDataStuffing", "oneByte= '.'" );
			// Read <CR><LF><DOT> so this line needs an additional period.
			super.write('.');
			state = STATE_NORMAL;
		} else {
			state = STATE_NORMAL;
		}
		super.write(oneByte);
	}
}
