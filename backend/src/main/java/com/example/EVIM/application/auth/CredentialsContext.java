package com.example.EVIM.application.auth;

public class CredentialsContext {

    public record JiraCredentials(String authHeader, String baseUrl) {}

    private static final ThreadLocal<JiraCredentials> holder = new ThreadLocal<>();

    public static void set(JiraCredentials credentials) {
        holder.set(credentials);
    }

    public static JiraCredentials get() {
        return holder.get();
    }

    public static void clear() {
        holder.remove();
    }
}
