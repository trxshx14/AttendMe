package edu.cit.cararag.attendme.features.report;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailServiceImpl implements EmailService {

    private final Resend resend;

    public EmailServiceImpl(@Value("${resend.api.key}") String apiKey) {
        this.resend = new Resend(apiKey);
    }

    // ─── Email 1: Welcome ────────────────────────────────────────────────────

    @Async
    @Override
    public void sendWelcomeEmail(String toEmail, String fullName, String username, String password) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("AttendMe <onboarding@resend.dev>")
                    .to(toEmail)
                    .subject("Welcome to AttendMe – Your Account is Ready")
                    .html(buildWelcomeEmailHtml(fullName, username, password))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Welcome email sent to {} | id: {}", toEmail, response.getId());

        } catch (ResendException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── Email 2: Password Updated ───────────────────────────────────────────

    @Async
    @Override
    public void sendPasswordUpdatedEmail(String toEmail, String fullName, String username, String newPassword) {
        try {
            CreateEmailOptions params = CreateEmailOptions.builder()
                    .from("AttendMe <onboarding@resend.dev>")
                    .to(toEmail)
                    .subject("AttendMe – Your Password Has Been Updated")
                    .html(buildPasswordUpdatedEmailHtml(fullName, username, newPassword))
                    .build();

            CreateEmailResponse response = resend.emails().send(params);
            log.info("Password updated email sent to {} | id: {}", toEmail, response.getId());

        } catch (ResendException e) {
            log.error("Failed to send password updated email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── HTML Templates ───────────────────────────────────────────────────────

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
                          <p style="margin:0;color:#94A3B8;font-size:13px;">— The AttendMe Team</p>
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

    private String buildPasswordUpdatedEmailHtml(String fullName, String username, String newPassword) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>Password Updated</title>
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
                            Hello, %s! 🔐
                          </p>
                          <p style="margin:0 0 28px;color:#64748B;font-size:15px;line-height:1.6;">
                            Your system administrator has updated your AttendMe password.
                            Below are your updated login credentials.
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
                                  New Password
                                </p>
                                <p style="margin:0;color:#0F2D5E;font-size:18px;font-weight:700;
                                          letter-spacing:2px;">
                                  %s
                                </p>
                              </td>
                            </tr>
                          </table>
                          <!-- Warning note -->
                          <table width="100%%" cellpadding="0" cellspacing="0"
                                 style="background-color:#FEF3C7;border-radius:10px;
                                        border-left:4px solid #F59E0B;margin-bottom:28px;">
                            <tr>
                              <td style="padding:16px 20px;">
                                <p style="margin:0;color:#92400E;font-size:13px;line-height:1.5;">
                                  ⚠️ If you did not request this change, please contact your
                                  system administrator immediately.
                                </p>
                              </td>
                            </tr>
                          </table>
                          <p style="margin:0;color:#94A3B8;font-size:13px;">— The AttendMe Team</p>
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
            """.formatted(fullName, username, newPassword);
    }
}