package org.phoebus.email;

import javax.mail.Authenticator;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * A service to handle email actions
 * @author Kunal Shroff
 *
 */
public class EmailService {

    private static final Logger logger = Logger.getLogger(EmailService.class.getName());
    private static final Session session;

    // TODO might have to rethink this static initialization of the session.
    static {
        final Properties props = new Properties();
        props.put("mail.smtp.host", EmailPreferences.mailhost);
        props.put("mail.smtp.port", EmailPreferences.mailport);

        final String username = EmailPreferences.username;
        final String password = EmailPreferences.password;

        if (!username.isEmpty() && !password.isEmpty()) {
            PasswordAuthentication auth = new PasswordAuthentication(username, password);
            session = Session.getDefaultInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return auth;
                }
            });
        } else {
            session = Session.getDefaultInstance(props);
        }
    }

    /**
     * Singleton
     */
    private EmailService()
    {

    }

    public static void send(String to, String from, String subject, String body) throws MessagingException
    {
        // Send the complete message parts
        BodyPart messageBodyPart;
        messageBodyPart = new MimeBodyPart();
        messageBodyPart.setText(body);
        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        EmailService.send(to, from, subject, multipart);
    }

    public static void send(String to, String from, String subject, Multipart body) throws MessagingException
    {
        Message message = new MimeMessage(session);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setFrom(new InternetAddress(from));
        message.setSubject(subject);
        // Send the complete message parts
        message.setContent(body);
        // Send message
        Transport.send(message);
    }

}
