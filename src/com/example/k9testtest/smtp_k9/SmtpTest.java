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
					 * 目前只有qq邮箱测试成功
					 */
					/**
					 * uri=smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25
					 * 
					 */
					 SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://devmail35:QWERTYUIOP:PLAIN@smtp.163.com:25");
					// SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://2726578812:QWERTYUIOP:PLAIN@smtp.qq.com:25");//尚无法开通smtp、imap服务
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp://1120078024:1044992985:PLAIN@smtp.qq.com:25");
//					SmtpTransport transport = (SmtpTransport) Transport.getInstance("smtp+ssl+://guangyu0:wszgy222,,!@smtp.gmail.com");

					MessageTemp msg = new MessageTemp();

					// 发件人
					Address[] froms = new Address[1];
//					froms[0] = new Address("1120078024@qq.com");
					froms[0] = new Address("devmail35@163.com");
//					froms[0] = new Address("guangyu0@gmail.com");
					msg.setFrom(froms);
					// 收件人-主送
					Address[] tos = new Address[2];
					tos[0] = new Address("devmail35@163.com");
					tos[1] = new Address("1044992985@qq.com");
					msg.setRecipients(RecipientType.TO, tos);
					
					// 收件人-抄送
					Address[] ccs = new Address[2];
					ccs[0] = new Address("devmail35@163.com");
					ccs[1] = new Address("1044992985@qq.com");
					msg.setRecipients(RecipientType.CC, ccs);
					
					// data命令之后的字段

					/**
					 * X-User-Agent: K-9 Mail for Android
					 * 
					 * MIME-Version: 1.0
					 * 
					 * Content-Type: text/plain;
					 * 
					 * Content-Transfer-Encoding: quoted-printable
					 * 
					 * 【Subject: kk】
					 * 
					 * 【From: dev <devmail35@163.com>】
					 * 
					 * Date: Tue, 30 Jul 2013 14:21:58 +0800
					 * 
					 * 【To: devmail35@163.com,1044992985@qq.com】
					 * 
					 * 【CC: devmail35@163.com,1044992985@qq.com】
					 * 
					 * Message-ID: <c6539cc0-fc8d-4693-aa4a-ae87a3d34b9f@email.android.com>
					 * 
					 * [B@41faa850
					 */
					MimeHeader headers = new MimeHeader();
//					headers.addHeader("From", "1120078024@qq.com");
//					headers.addHeader("From", "guangyu0@gmail.com");
					headers.addHeader("From", "devmail35@163.com");
					headers.addHeader("To", "devmail35@163.com,1044992985@qq.com");
					headers.addHeader("CC", "devmail35@163.com,1044992985@qq.com");
					headers.addHeader("Subject", "subject主题");
					msg.setHeaders(headers);
					// 邮件内容
					msg.setBody("this is mail content内容");

					transport.sendMessage(msg);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}
}
