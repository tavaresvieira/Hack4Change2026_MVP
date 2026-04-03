package org.resourcebridge.api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.resourcebridge.api.entity.Donation;
import org.resourcebridge.api.entity.Transfer;
import org.resourcebridge.api.enums.DonationStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${mail.enabled:false}")
    private boolean enabled;

    @Value("${mail.from:ResourceBridge <noreply@resourcebridge.ca>}")
    private String fromAddress;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    // ── Invite email ──────────────────────────────────────────────────────────

    @Async
    public void sendInviteEmail(String toEmail, String inviteToken, String organizationName, String role) {
        if (!enabled) {
            log.info("[Email disabled] Would send invite to {} for org '{}' role {}", toEmail, organizationName, role);
            return;
        }

        String inviteUrl = baseUrl + "/register?token=" + inviteToken;
        boolean isAdmin = "ADMIN".equals(role);
        String roleLabel = isAdmin ? "Admin" : "Shelter Staff";
        String accentColor = isAdmin ? "#7c3aed" : "#16a34a";

        String subject = "You've been invited to join ResourceBridge as " + roleLabel;
        String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f9fafb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f9fafb;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb;">

                        <!-- Header -->
                        <tr><td style="background:%s;padding:28px 32px;">
                          <span style="color:#ffffff;font-size:20px;font-weight:700;">ResourceBridge</span>
                        </td></tr>

                        <!-- Body -->
                        <tr><td style="padding:32px;">
                          <h1 style="margin:0 0 8px;font-size:22px;color:#111827;">You've been invited!</h1>
                          <p style="margin:0 0 24px;color:#6b7280;font-size:15px;line-height:1.6;">
                            You've been invited to join <strong style="color:#111827;">%s</strong>
                            on ResourceBridge as <strong style="color:%s;">%s</strong>.
                          </p>
                          <p style="margin:0 0 28px;color:#6b7280;font-size:14px;line-height:1.6;">
                            Click the button below to set up your account. The link expires in <strong>7 days</strong>
                            and can only be used once.
                          </p>

                          <!-- CTA Button -->
                          <table cellpadding="0" cellspacing="0">
                            <tr><td style="border-radius:8px;background:%s;">
                              <a href="%s" style="display:inline-block;padding:14px 28px;color:#ffffff;font-size:15px;font-weight:600;text-decoration:none;">
                                Accept Invitation →
                              </a>
                            </td></tr>
                          </table>

                          <p style="margin:28px 0 0;font-size:12px;color:#9ca3af;">
                            Or copy this link: <a href="%s" style="color:#6b7280;">%s</a>
                          </p>
                        </td></tr>

                        <!-- Footer -->
                        <tr><td style="padding:20px 32px;background:#f9fafb;border-top:1px solid #f3f4f6;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;">
                            This invite was sent by a ResourceBridge admin. If you didn't expect this, you can ignore it.
                          </p>
                        </td></tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(accentColor, organizationName, accentColor, roleLabel,
                             accentColor, inviteUrl, inviteUrl, inviteUrl);

        send(toEmail, subject, html);
    }

    // ── Donation status emails ────────────────────────────────────────────────

    @Async
    public void sendDonationStatusEmail(Donation donation) {
        if (!enabled) {
            log.info("[Email disabled] Would send donation status '{}' to {}", donation.getStatus(), donation.getDonorEmail());
            return;
        }
        if (donation.getDonorEmail() == null) return;

        String subject = buildStatusSubject(donation);
        if (subject == null) return; // status doesn't warrant an email

        String html = buildStatusHtml(donation);
        send(donation.getDonorEmail(), subject, html);
    }

    private String buildStatusSubject(Donation donation) {
        String item = donation.getItem() != null ? donation.getItem().getName() : "your donation";
        return switch (donation.getStatus()) {
            case ASSIGNED  -> "Your " + item + " donation has been matched — ResourceBridge";
            case DELIVERED -> "Your " + item + " donation is on its way — ResourceBridge";
            case RECEIVED  -> "Thank you! Your " + item + " donation was received — ResourceBridge";
            default        -> null;
        };
    }

    private String buildStatusHtml(Donation donation) {
        String donorName = donation.getDonorName() != null ? donation.getDonorName().split(" ")[0] : "there";
        String item = donation.getItem() != null ? donation.getItem().getName() : "donation";
        int qty = donation.getQuantity();

        String headline, message, icon;
        switch (donation.getStatus()) {
            case ASSIGNED -> {
                icon = "🎯";
                headline = "Your donation has been matched!";
                message = "Great news, " + donorName + "! Your <strong>" + qty + " " + item + "</strong> "
                        + "has been matched to a shelter in our network that needs it most. "
                        + "We'll let you know once it's been delivered and received.";
            }
            case DELIVERED -> {
                icon = "🚚";
                headline = "Your donation is on its way!";
                message = "Hi " + donorName + "! Your <strong>" + qty + " " + item + "</strong> "
                        + "is now in transit to the shelter. "
                        + "We'll send you one more update once it's been received.";
            }
            case RECEIVED -> {
                icon = "✅";
                headline = "Your donation was received — thank you!";
                message = "Hi " + donorName + "! Your <strong>" + qty + " " + item + "</strong> "
                        + "has been received by the shelter and will go directly to people in need. "
                        + "Your generosity makes a real difference. Thank you! 💚";
            }
            default -> {
                icon = "📦";
                headline = "Donation update";
                message = "There's an update on your donation of " + qty + " " + item + ".";
            }
        }

        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f9fafb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f9fafb;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb;">

                        <!-- Header -->
                        <tr><td style="background:#16a34a;padding:28px 32px;">
                          <span style="color:#ffffff;font-size:20px;font-weight:700;">ResourceBridge</span>
                        </td></tr>

                        <!-- Body -->
                        <tr><td style="padding:32px;">
                          <div style="font-size:40px;margin-bottom:16px;">%s</div>
                          <h1 style="margin:0 0 16px;font-size:22px;color:#111827;">%s</h1>
                          <p style="margin:0 0 24px;color:#6b7280;font-size:15px;line-height:1.6;">%s</p>
                          <p style="margin:0;font-size:13px;color:#9ca3af;">
                            You can track all your donations at
                            <a href="%s/donor" style="color:#16a34a;">%s/donor</a>
                          </p>
                        </td></tr>

                        <!-- Footer -->
                        <tr><td style="padding:20px 32px;background:#f9fafb;border-top:1px solid #f3f4f6;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;">
                            You received this because you donated through ResourceBridge.
                          </p>
                        </td></tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(icon, headline, message, baseUrl, baseUrl);
    }

    // ── Staff match notification ──────────────────────────────────────────────

    @Async
    public void sendStaffMatchNotification(java.util.List<String> staffEmails, Transfer transfer) {
        if (!enabled) {
            log.info("[Email disabled] Would notify {} staff at '{}' about matched transfer",
                    staffEmails.size(), transfer.getToOrganization().getName());
            return;
        }
        if (staffEmails.isEmpty()) return;

        String itemName = transfer.getDonation().getItem() != null
                ? transfer.getDonation().getItem().getName() : "items";
        int qty = transfer.getQuantityAssigned();
        String orgName = transfer.getToOrganization().getName();
        String donorName = transfer.getDonation().getDonorName();
        String donorPhone = transfer.getDonation().getDonorPhone();
        String donationType = transfer.getDonation().getDonationType() != null
                ? transfer.getDonation().getDonationType().name() : "DROP_OFF";

        boolean isPickup = "PICKUP_REQUEST".equals(donationType);
        String pickupInfo = isPickup && transfer.getDonation().getPickupAddress() != null
                ? transfer.getDonation().getPickupAddress() + (transfer.getDonation().getPickupCity() != null
                  ? ", " + transfer.getDonation().getPickupCity() : "")
                : null;

        String subject = "New donation matched to " + orgName + " — ResourceBridge";

        String html = """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f9fafb;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f9fafb;padding:40px 0;">
                    <tr><td align="center">
                      <table width="560" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e5e7eb;">

                        <!-- Header -->
                        <tr><td style="background:#16a34a;padding:28px 32px;">
                          <span style="color:#ffffff;font-size:20px;font-weight:700;">ResourceBridge</span>
                        </td></tr>

                        <!-- Body -->
                        <tr><td style="padding:32px;">
                          <div style="font-size:36px;margin-bottom:16px;">📦</div>
                          <h1 style="margin:0 0 8px;font-size:22px;color:#111827;">New donation incoming!</h1>
                          <p style="margin:0 0 24px;color:#6b7280;font-size:15px;line-height:1.6;">
                            A donation of <strong style="color:#111827;">%d %s</strong> has been matched to
                            <strong style="color:#111827;">%s</strong> and is on its way.
                          </p>

                          <!-- Details card -->
                          <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f9fafb;border-radius:10px;border:1px solid #e5e7eb;margin-bottom:24px;">
                            <tr><td style="padding:20px;">
                              <table width="100%%" cellpadding="0" cellspacing="0">
                                <tr>
                                  <td style="padding:6px 0;color:#6b7280;font-size:14px;width:140px;">Donor</td>
                                  <td style="padding:6px 0;color:#111827;font-size:14px;font-weight:500;">%s</td>
                                </tr>
                                %s
                                <tr>
                                  <td style="padding:6px 0;color:#6b7280;font-size:14px;">Item</td>
                                  <td style="padding:6px 0;color:#111827;font-size:14px;font-weight:500;">%d × %s</td>
                                </tr>
                                <tr>
                                  <td style="padding:6px 0;color:#6b7280;font-size:14px;">Delivery</td>
                                  <td style="padding:6px 0;color:#111827;font-size:14px;font-weight:500;">%s</td>
                                </tr>
                                %s
                              </table>
                            </td></tr>
                          </table>

                          <p style="margin:0;font-size:13px;color:#9ca3af;line-height:1.6;">
                            Log in to <a href="%s/login" style="color:#16a34a;">ResourceBridge</a>
                            to confirm receipt once the donation arrives.
                          </p>
                        </td></tr>

                        <!-- Footer -->
                        <tr><td style="padding:20px 32px;background:#f9fafb;border-top:1px solid #f3f4f6;">
                          <p style="margin:0;font-size:12px;color:#9ca3af;">
                            You received this as a staff member at %s on ResourceBridge.
                          </p>
                        </td></tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(
                qty, itemName, orgName,
                donorName,
                donorPhone != null
                        ? "<tr><td style=\"padding:6px 0;color:#6b7280;font-size:14px;\">Phone</td><td style=\"padding:6px 0;color:#111827;font-size:14px;font-weight:500;\">" + donorPhone + "</td></tr>"
                        : "",
                qty, itemName,
                isPickup ? "Pickup request" : "Drop-off",
                pickupInfo != null
                        ? "<tr><td style=\"padding:6px 0;color:#6b7280;font-size:14px;\">Address</td><td style=\"padding:6px 0;color:#111827;font-size:14px;font-weight:500;\">" + pickupInfo + "</td></tr>"
                        : "",
                baseUrl,
                orgName
        );

        for (String email : staffEmails) {
            send(email, subject, html);
        }
    }

    // ── Internal send helper ──────────────────────────────────────────────────

    private void send(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
