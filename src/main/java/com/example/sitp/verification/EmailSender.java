package com.example.sitp.verification;

public interface EmailSender {

    void send(String to, String subject, String body);
}
