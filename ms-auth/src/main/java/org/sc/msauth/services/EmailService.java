package org.sc.msauth.services;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    public void sendVerificationEmail(String to, String token, String verificationUrl) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);

        // Make sure this domain is verified in DNS if not using Gmail

        // Optional but useful
        helper.setReplyTo("support@supplycrate.com");

        helper.setSubject("Verify Your Email Address");

        String verificationLink = verificationUrl + "?token=" + token;
        String htmlContent =
                "<!DOCTYPE html>" +
                        "<html>" +
                        "<head>" +
                        "<meta charset='UTF-8'>" +
                        "<style>" +
                        "  body { font-family: Arial, sans-serif; color: #333; }" +
                        "  .container { padding: 20px; background: #f9f9f9; border-radius: 10px; max-width: 600px; margin: auto; }" +
                        "  a.button { display: inline-block; padding: 10px 15px; background-color: #007bff; color: white; text-decoration: none; border-radius: 5px; }" +
                        "</style>" +
                        "</head>" +
                        "<body>" +
                        "<div class='container'>" +
                        "<h2>Verify Your Email</h2>" +
                        "<p>Thank you for registering with <strong>SupplyCrate</strong>.</p>" +
                        "<p>Please confirm your email address by clicking the button below:</p>" +
                        "<p><a href='" + verificationLink + "' class='button'>Verify Email</a></p>" +
                        "<p>If the button doesn't work, copy and paste the following link into your browser:</p>" +
                        "<p><a href='" + verificationLink + "'>" + verificationLink + "</a></p>" +
                        "<p>This link will expire in 24 hours.</p>" +
                        "</div>" +
                        "</body>" +
                        "</html>";

        helper.setText(htmlContent, true);
        mailSender.send(message);
    }

}