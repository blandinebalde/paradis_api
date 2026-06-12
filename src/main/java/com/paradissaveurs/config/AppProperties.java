package com.paradissaveurs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Cors cors = new Cors();
    private Notifications notifications = new Notifications();
    private String uploadDir = "uploads";

    public Jwt getJwt() { return jwt; }
    public void setJwt(Jwt jwt) { this.jwt = jwt; }
    public Cors getCors() { return cors; }
    public void setCors(Cors cors) { this.cors = cors; }
    public Notifications getNotifications() { return notifications; }
    public void setNotifications(Notifications notifications) { this.notifications = notifications; }
    public String getUploadDir() { return uploadDir; }
    public void setUploadDir(String uploadDir) { this.uploadDir = uploadDir; }

    public static class Jwt {
        private String secret;
        private long expirationMs = 28800000;
        private long cleanupIntervalMs = 3600000;

        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
        public long getExpirationMs() { return expirationMs; }
        public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
        public long getCleanupIntervalMs() { return cleanupIntervalMs; }
        public void setCleanupIntervalMs(long cleanupIntervalMs) { this.cleanupIntervalMs = cleanupIntervalMs; }
    }

    public static class Cors {
        private List<String> allowedOrigins = List.of("http://localhost:5173");

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
    }

    public static class Notifications {
        private Twilio twilio = new Twilio();
        private Firebase firebase = new Firebase();

        public Twilio getTwilio() { return twilio; }
        public void setTwilio(Twilio twilio) { this.twilio = twilio; }
        public Firebase getFirebase() { return firebase; }
        public void setFirebase(Firebase firebase) { this.firebase = firebase; }
    }

    public static class Firebase {
        private String credentialsPath = "";

        public String getCredentialsPath() { return credentialsPath; }
        public void setCredentialsPath(String credentialsPath) { this.credentialsPath = credentialsPath; }
    }

    public static class Twilio {
        private String accountSid = "";
        private String authToken = "";
        /** Ex: whatsapp:+14155238886 (sandbox Twilio) */
        private String whatsappFrom = "";
        /** Ex: +1234567890 */
        private String smsFrom = "";

        public String getAccountSid() { return accountSid; }
        public void setAccountSid(String accountSid) { this.accountSid = accountSid; }
        public String getAuthToken() { return authToken; }
        public void setAuthToken(String authToken) { this.authToken = authToken; }
        public String getWhatsappFrom() { return whatsappFrom; }
        public void setWhatsappFrom(String whatsappFrom) { this.whatsappFrom = whatsappFrom; }
        public String getSmsFrom() { return smsFrom; }
        public void setSmsFrom(String smsFrom) { this.smsFrom = smsFrom; }
    }
}
