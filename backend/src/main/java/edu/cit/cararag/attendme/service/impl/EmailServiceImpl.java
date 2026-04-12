package edu.cit.cararag.attendme.service.impl;

import edu.cit.cararag.attendme.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Async
    @Override
    public void sendWelcomeEmail(String toEmail, String fullName, String username, String temporaryPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom("cararagtrisharaye@gmail.com");
            helper.setTo(toEmail);
            helper.setSubject("Welcome to AttendMe – Your Account is Ready");
            helper.setText(buildWelcomeEmailHtml(fullName, username, temporaryPassword), true);

            mailSender.send(message);
            log.info("Welcome email sent to {}", toEmail);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    private String buildWelcomeEmailHtml(String fullName, String username, String password) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Welcome to AttendMe</title>
            </head>
            <body style="margin:0;padding:0;background-color:#F4F7FB;font-family:Arial,sans-serif;">
              <table width="100%%" cellpadding="0" cellspacing="0" style="background-color:#F4F7FB;padding:40px 0;">
                <tr>
                  <td align="center">
                    <table width="600" cellpadding="0" cellspacing="0"
                           style="background-color:#ffffff;border-radius:16px;overflow:hidden;
                                  box-shadow:0 4px 20px rgba(0,0,0,0.08);">

                      <!-- Header -->
                      <tr>
                        <td style="background-color:#0F2D5E;padding:32px 40px;text-align:center;">
                          <h1 style="margin:0;color:#ffffff;font-size:28px;font-weight:700;
                                     letter-spacing:1px;">AttendMe</h1>
                          <p style="margin:6px 0 0;color:#93C5FD;font-size:14px;">
                            Attendance Management System
                          </p>
                        </td>
                      </tr>

                      <!-- Body -->
                      <tr>
                        <td style="padding:40px;">

                          <p style="margin:0 0 8px;color:#0F2D5E;font-size:22px;font-weight:700;">
                            Welcome, %s! 👋
                          </p>
                          <p style="margin:0 0 28px;color:#64748B;font-size:15px;line-height:1.6;">
                            Your account has been created by the system administrator in your school.
                            You can now log in to AttendMe using the credentials below.
                          </p>

                          <!-- Credentials Box -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                              style="background-color:#EBF2FF;border-radius:12px;
                                        border-left:4px solid #0F2D5E;margin-bottom:28px;">
                            <tr>
                              <td style="padding:24px 28px;">
                                <p style="margin:0 0 4px;color:#64748B;font-size:12px;
                                          font-weight:700;letter-spacing:0.06em;text-transform:uppercase;">
                                  Username
                                </p>
                                <p style="margin:0 0 20px;color:#0F2D5E;font-size:18px;font-weight:700;">
                                  %s
                                </p>
                                <p style="margin:0 0 4px;color:#64748B;font-size:12px;
                                          font-weight:700;letter-spacing:0.06em;text-transform:uppercase;">
                                  Password
                                </p>
                                <p style="margin:0;color:#0F2D5E;font-size:18px;font-weight:700;
                                          letter-spacing:2px;">
                                  %s
                                </p>
                              </td>
                            </tr>
                          </table>

                          <!-- Info note -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background-color:#EBF2FF;border-radius:10px;
                                        border-left:4px solid #0F2D5E;margin-bottom:28px;">
                            <tr>
                              <td style="padding:16px 20px;">
                                <p style="margin:0;color:#0F2D5E;font-size:13px;line-height:1.5;">
                                  ℹ️ Your login credentials are managed by your system administrator.
                                  If you have concerns about your account, please contact them directly.
                                </p>
                              </td>
                            </tr>
                          </table>

                          <p style="margin:0 0 28px;color:#64748B;font-size:14px;line-height:1.6;">
                            If you have any questions or need assistance, please contact your system administrator.
                          </p>

                          <p style="margin:0;color:#94A3B8;font-size:13px;">
                            — The AttendMe Team
                          </p>

                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td style="background-color:#F8FAFC;padding:20px 40px;text-align:center;
                                border-top:1px solid #E2E8F0;">
                          <p style="margin:0;color:#94A3B8;font-size:12px;">
                            This is an automated message. Please do not reply to this email.
                          </p>
                          <p style="margin:6px 0 0;color:#94A3B8;font-size:12px;">
                            © 2026 AttendMe · Attendance Management System
                          </p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(fullName, username, password);
    }
}