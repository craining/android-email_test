package com.example.k9testtest.smtp_k9;

import com.example.k9testtest.smtp_k9.MessageTemp.RecipientType;

public class SmtpTest {

	/**
	 * ���ʼ����Է���
	 * 
	 * smtp���ʼ���uri��ʽ; {@link SmtpTransport#SmtpTransport(String)}������ע��
	 * 
	 * @Description:
	 * @see:
	 * @since:
	 * @author: zhuanggy
	 * @date:2013-7-30
	 */
	public static void doSmtpCommand() {

		
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {

					/**
					 * Ŀǰ163�����⣬��ʱ��qq�������
					 */
					/**
					 * uri=smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25
					 * 
					 */
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25");
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://2726578812:QWERTYUIOP:PLAIN@smtp.qq.com:25");//���޷���ͨsmtp��imap����
					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://1120078024:1044992985:PLAIN@smtp.qq.com:25");
					
					 MessageTemp msg = new MessageTemp();
					 
					 //������
					 Address[] froms = new Address[1];
					 froms[0] = new Address("1120078024@qq.com");
					 msg.setFrom(froms);
					 //�ռ���
					 Address[] tos = new Address[2];
					 tos[0] = new Address("devmail35@163.com");
					 tos[1] = new Address("1044992985@qq.com");
					 msg.setRecipients(RecipientType.TO, tos);
					 //data����֮����ֶ�
					 MimeHeader headers = new MimeHeader();
					 headers.addHeader("from", "1120078024@qq.com");
//					 headers.addHeader("to", "");//���Բ���Ҫ
					 headers.addHeader("subject", "subject����");
					 msg.setHeaders(headers);
					 //�ʼ�����
					 msg.setBody("this is mail content����");
					 
					 
					 transport.sendMessage( msg );
					 
					 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
