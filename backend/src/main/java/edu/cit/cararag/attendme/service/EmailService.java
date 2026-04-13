package edu.cit.cararag.attendme.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String fullName, String username, String password);
    void sendPasswordUpdatedEmail(String toEmail, String fullName, String username, String newPassword);
}