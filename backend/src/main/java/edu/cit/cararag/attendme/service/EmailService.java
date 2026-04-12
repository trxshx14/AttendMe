package edu.cit.cararag.attendme.service;

public interface EmailService {
    void sendWelcomeEmail(String toEmail, String fullName, String username, String temporaryPassword);
}