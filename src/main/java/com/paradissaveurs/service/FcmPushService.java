package com.paradissaveurs.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.paradissaveurs.config.AppProperties;
import com.paradissaveurs.entity.OrderEntity;
import com.paradissaveurs.repository.AdminDeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

@Service
public class FcmPushService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushService.class);

    private final AdminDeviceTokenRepository deviceTokenRepository;
    private final AppProperties appProperties;
    private boolean firebaseReady = false;

    public FcmPushService(AdminDeviceTokenRepository deviceTokenRepository, AppProperties appProperties) {
        this.deviceTokenRepository = deviceTokenRepository;
        this.appProperties = appProperties;
    }

    @PostConstruct
    void init() {
        var fcm = appProperties.getNotifications().getFirebase();
        if (!StringUtils.hasText(fcm.getCredentialsPath())) {
            log.info("FCM non configuré — notifications push mobile désactivées (définir app.notifications.firebase.credentials-path)");
            return;
        }
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                var stream = new FileInputStream(fcm.getCredentialsPath());
                var options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(stream))
                        .build();
                FirebaseApp.initializeApp(options);
            }
            firebaseReady = true;
            log.info("Firebase Admin initialisé pour les notifications push");
        } catch (Exception ex) {
            log.error("Impossible d'initialiser Firebase Admin : {}", ex.getMessage());
        }
    }

    public void notifyNewOrder(OrderEntity order) {
        if (!firebaseReady) return;

        var tokens = deviceTokenRepository.findAll();
        if (tokens.isEmpty()) {
            log.debug("Aucun appareil admin enregistré pour FCM");
            return;
        }

        String title = "🛒 Nouvelle commande " + order.getId();
        String body = buildNotificationBody(order);

        for (var device : tokens) {
            try {
                Map<String, String> data = new HashMap<>();
                data.put("orderId", order.getId());
                data.put("type", "new_order");

                Message message = Message.builder()
                        .setToken(device.getToken())
                        .setNotification(Notification.builder()
                                .setTitle(title)
                                .setBody(body)
                                .build())
                        .putAllData(data)
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("Push FCM envoyé à {}", device.getPlatform());
            } catch (FirebaseMessagingException ex) {
                log.warn("Échec push FCM (token {}...) : {}", device.getToken().substring(0, Math.min(8, device.getToken().length())), ex.getMessage());
                if (isInvalidToken(ex)) {
                    deviceTokenRepository.delete(device);
                }
            }
        }
    }

    private boolean isInvalidToken(FirebaseMessagingException ex) {
        var code = ex.getMessagingErrorCode();
        return code != null && (code.name().contains("UNREGISTERED") || code.name().contains("INVALID"));
    }

    private String formatPrice(Integer amount) {
        if (amount == null) return "0 FCFA";
        return String.format("%,d FCFA", amount);
    }

    private String buildNotificationBody(OrderEntity order) {
        var sb = new StringBuilder();
        sb.append("👤 ").append(order.getCustomerName()).append('\n');
        if (StringUtils.hasText(order.getCustomerPhone())) {
            sb.append("📞 ").append(order.getCustomerPhone()).append('\n');
        }
        if ("delivery".equals(order.getDeliveryMode())) {
            sb.append("🛵 Livraison");
            if (StringUtils.hasText(order.getCustomerAddress())) {
                sb.append(" — 📍 ").append(order.getCustomerAddress());
            }
            sb.append('\n');
            if (order.getDeliveryFee() != null && order.getDeliveryFee() > 0) {
                sb.append("Frais livraison : ").append(formatPrice(order.getDeliveryFee())).append('\n');
            }
        } else {
            sb.append("🏪 Collecte sur place\n");
        }
        sb.append(paymentLabel(order.getPaymentMethod())).append('\n');
        sb.append('\n').append("🍽️ Articles :\n");
        var items = order.getItems();
        int shown = 0;
        for (var item : items) {
            if (shown >= 8) break;
            String emoji = StringUtils.hasText(item.getEmoji()) ? item.getEmoji() : "•";
            int lineTotal = (item.getPrice() != null ? item.getPrice() : 0) * (item.getQuantity() != null ? item.getQuantity() : 0);
            sb.append(emoji).append(' ')
                    .append(item.getQuantity()).append("× ")
                    .append(item.getName()).append(" — ")
                    .append(formatPrice(lineTotal)).append('\n');
            shown++;
        }
        if (items.size() > 8) {
            sb.append("… et ").append(items.size() - 8).append(" autre(s)\n");
        }
        sb.append('\n').append("💰 Total : ").append(formatPrice(order.getTotal()));
        if (StringUtils.hasText(order.getNotes())) {
            sb.append('\n').append("📝 ").append(order.getNotes());
        }
        return sb.toString();
    }

    private String paymentLabel(String method) {
        if (method == null) return "";
        return switch (method) {
            case "livraison" -> "💵 À la livraison";
            case "wave" -> "📱 Wave";
            case "om" -> "🟠 Orange Money";
            default -> method;
        };
    }
}
