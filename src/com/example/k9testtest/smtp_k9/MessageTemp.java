package com.example.k9testtest.smtp_k9;

public class MessageTemp {

	
	private String body = "";
	
	private AddressTemp[] from;

	private AddressTemp[] to;
	private AddressTemp[] cc;
	private AddressTemp[] bcc;

	public AddressTemp[] getFrom() {
		return from;
	}

	public void setFrom(AddressTemp[] fromAddrs) {
		this.from = fromAddrs;
	}

	public AddressTemp[] getRecipients(int type) {
		switch (type) {
		case RecipientType.TO:

			return to;
		case RecipientType.CC:

			return cc;

		case RecipientType.BCC:

			return bcc;
		default:
			break;
		}

		return null;
	}

	public void setRecipients(int type, AddressTemp[] addrs) {
		switch (type) {
		case RecipientType.TO:

			this.to = addrs;
			break;
		case RecipientType.CC:

			this.cc = addrs;
			break;
		case RecipientType.BCC:

			this.bcc = addrs;
			break;
		default:
			break;
		}
	}

	
	
	public String getBody() {
		return body;
	}

	
	public void setBody(String body) {
		this.body = body;
	}
	
	
	public class RecipientType {

		public static final int TO = 0;
		public static final int CC = 1;
		public static final int BCC = 2;

	}

}
