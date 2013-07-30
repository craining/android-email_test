package com.example.k9testtest.smtp_k9;

import com.example.k9testtest.smtp_k9.MessageTemp.RecipientType;

public class SmtpTest {

	/**
	 * 发邮件测试方法
	 * 
	 * smtp发邮件的uri格式; {@link SmtpTransport#SmtpTransport(String)}方法的注释
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
					 * 目前163有问题，暂时用qq邮箱测试
					 */
					/**
					 * uri=smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25
					 * 
					 */
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25");
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://2726578812:QWERTYUIOP:PLAIN@smtp.qq.com:25");//尚无法开通smtp、imap服务
					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://1120078024:1044992985:PLAIN@smtp.qq.com:25");
					
					 MessageTemp msg = new MessageTemp();
					 
					 //发件人
					 Address[] froms = new Address[1];
					 froms[0] = new Address("1120078024@qq.com");
					 msg.setFrom(froms);
					 //收件人
					 Address[] tos = new Address[2];
					 tos[0] = new Address("devmail35@163.com");
					 tos[1] = new Address("1044992985@qq.com");
					 msg.setRecipients(RecipientType.TO, tos);
					 //data命令之后的字段
					 MimeHeader headers = new MimeHeader();
					 headers.addHeader("from", "1120078024@qq.com");
//					 headers.addHeader("to", "");//可以不需要
					 headers.addHeader("subject", "subject主题");
					 msg.setHeaders(headers);
					 //邮件内容
					 msg.setBody("this is mail content内容");
					 
					 
					 transport.sendMessage( msg );
					 
					 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
