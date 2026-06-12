package com.paradissaveurs.service;

import com.paradissaveurs.config.AppProperties;
import com.paradissaveurs.entity.OrderEntity;
import com.paradissaveurs.entity.OrderItemEntity;
import com.paradissaveurs.entity.ShopSettingsEntity;
import com.paradissaveurs.repository.DeliveryZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

@Service
public class OrderNotificationService {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationService.class);

    private static final Map<String, String> PAYMENT_LABELS = Map.of(
            "livraison", "À la livraison",
            "wave", "Wave",
            "om", "Orange Money"
    );

    private static final Map<String, String> DELIVERY_LABELS = Map.of(
            "delivery", "Livraison",
            "pickup", "Collect"
    );

    private final AppProperties appProperties;
    private final DeliveryZoneRepository zoneRepository;
    private final RestClient restClient;

    public OrderNotificationService(AppProperties appProperties, DeliveryZoneRepository zoneRepository) {
        this.appProperties = appProperties;
        this.zoneRepository = zoneRepository;
        this.restClient = RestClient.create();
    }

    public NotificationResult notifyAdmin(OrderEntity order, ShopSettingsEntity settings) {
        String message = buildMessage(order);
        String adminPhone = resolveAdminPhone(settings);
        if (!StringUtils.hasText(adminPhone)) {
            log.warn("Notification admin ignorée : numéro admin non configuré (commande {})", order.getId());
            return new NotificationResult(false, "none");
        }

        var twilio = appProperties.getNotifications().getTwilio();
        boolean twilioReady = StringUtils.hasText(twilio.getAccountSid())
                && StringUtils.hasText(twilio.getAuthToken());

        if (Boolean.TRUE.equals(settings.getNotifyViaWhatsApp())) {
            if (twilioReady && StringUtils.hasText(twilio.getWhatsappFrom())) {
                boolean sent = sendTwilioMessage(
                        twilio,
                        "whatsapp:" + formatE164(adminPhone),
                        twilio.getWhatsappFrom(),
                        message
                );
                if (sent) return new NotificationResult(true, "whatsapp");
            }
            log.info("[NOTIF WhatsApp simulée — configurez Twilio] Commande {}\n{}", order.getId(), message);
            return new NotificationResult(true, "log");
        }

        if (Boolean.TRUE.equals(settings.getNotifyViaSms())) {
            if (twilioReady && StringUtils.hasText(twilio.getSmsFrom())) {
                boolean sent = sendTwilioMessage(
                        twilio,
                        formatE164(adminPhone),
                        twilio.getSmsFrom(),
                        message
                );
                if (sent) return new NotificationResult(true, "sms");
            }
            log.info("[NOTIF SMS simulée — configurez Twilio] Commande {}\n{}", order.getId(), message);
            return new NotificationResult(true, "log");
        }

        log.debug("Notifications désactivées pour la commande {}", order.getId());
        return new NotificationResult(false, "none");
    }

    private boolean sendTwilioMessage(AppProperties.Twilio twilio, String to, String from, String body) {
        try {
            MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
            form.add("To", to);
            form.add("From", from);
            form.add("Body", body);

            String auth = twilio.getAccountSid() + ":" + twilio.getAuthToken();
            String encoded = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

            restClient.post()
                    .uri("https://api.twilio.com/2010-04-01/Accounts/{sid}/Messages.json", twilio.getAccountSid())
                    .header("Authorization", "Basic " + encoded)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification envoyée à {} pour commande", to);
            return true;
        } catch (Exception ex) {
            log.error("Échec envoi notification Twilio vers {} : {}", to, ex.getMessage());
            return false;
        }
    }

    String buildMessage(OrderEntity order) {
        StringBuilder sb = new StringBuilder();
        sb.append("🛒 Nouvelle commande — Paradis des Saveurs\n\n");
        sb.append("N° ").append(order.getId()).append("\n");
        sb.append("👤 ").append(order.getCustomerName()).append("\n");
        sb.append("📞 ").append(order.getCustomerPhone()).append("\n");

        String mode = DELIVERY_LABELS.getOrDefault(order.getDeliveryMode(), order.getDeliveryMode());
        String zoneName = order.getZoneId() != null
                ? zoneRepository.findById(order.getZoneId()).map(z -> z.getName()).orElse(null)
                : null;
        sb.append("📦 ").append(mode);
        if (zoneName != null) sb.append(" — ").append(zoneName);
        sb.append("\n");

        if (StringUtils.hasText(order.getCustomerAddress())) {
            sb.append("📍 ").append(order.getCustomerAddress()).append("\n");
        }

        String payment = PAYMENT_LABELS.getOrDefault(order.getPaymentMethod(), order.getPaymentMethod());
        sb.append("💳 ").append(payment).append("\n\n");
        sb.append("Articles :\n");

        for (OrderItemEntity item : order.getItems()) {
            int lineTotal = (item.getPrice() != null ? item.getPrice() : 0) * item.getQuantity();
            sb.append("• ")
                    .append(item.getEmoji() != null ? item.getEmoji() : "🍽️")
                    .append(" ")
                    .append(item.getName())
                    .append(" ×")
                    .append(item.getQuantity())
                    .append(" — ")
                    .append(formatPrice(lineTotal))
                    .append("\n");
        }

        sb.append("\nSous-total : ").append(formatPrice(order.getSubtotal()));
        if (order.getDeliveryFee() != null && order.getDeliveryFee() > 0) {
            sb.append("\nLivraison : ").append(formatPrice(order.getDeliveryFee()));
        }
        sb.append("\nTotal : ").append(formatPrice(order.getTotal()));

        if (StringUtils.hasText(order.getNotes())) {
            sb.append("\n\n📝 ").append(order.getNotes());
        }

        return sb.toString();
    }

    private String resolveAdminPhone(ShopSettingsEntity settings) {
        if (StringUtils.hasText(settings.getAdminNotifyPhone())) {
            return settings.getAdminNotifyPhone();
        }
        return settings.getShopPhone();
    }

    private String formatE164(String phone) {
        String digits = phone.replaceAll("\\D", "");
        if (digits.startsWith("221")) return "+" + digits;
        if (digits.startsWith("0")) return "+221" + digits.substring(1);
        if (digits.length() == 9) return "+221" + digits;
        return "+" + digits;
    }

    private String formatPrice(Integer amount) {
        if (amount == null) return "0 FCFA";
        return String.format(Locale.FRANCE, "%,d FCFA", amount);
    }

    public record NotificationResult(boolean sent, String channel) {}
}
