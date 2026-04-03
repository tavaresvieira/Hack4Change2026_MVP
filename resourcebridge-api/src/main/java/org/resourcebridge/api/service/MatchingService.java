package org.resourcebridge.api.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.Donation;
import org.resourcebridge.api.entity.Need;
import org.resourcebridge.api.entity.Transfer;
import org.resourcebridge.api.enums.DonationStatus;
import org.resourcebridge.api.enums.Role;
import org.resourcebridge.api.enums.TransferStatus;
import org.resourcebridge.api.repository.DonationRepository;
import org.resourcebridge.api.repository.NeedRepository;
import org.resourcebridge.api.repository.TransferRepository;
import org.resourcebridge.api.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final NeedRepository needRepository;
    private final TransferRepository transferRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Called after a donation is submitted.
     * Finds the highest-urgency unfulfilled need for the same item
     * and automatically creates a transfer to that shelter.
     */
    @Transactional
    public Transfer autoMatch(Donation donation) {
        List<Need> needs;

        // If the donor chose a specific shelter, try matching there first
        if (donation.getPreferredOrganization() != null) {
            needs = needRepository.findUnfulfilledByItemAndOrgOrderByUrgency(
                    donation.getItem().getId(),
                    donation.getPreferredOrganization().getId()
            );
            // Fall back to global match if that shelter doesn't need this item
            if (needs.isEmpty()) {
                needs = needRepository.findUnfulfilledByItemOrderByUrgency(donation.getItem().getId());
            }
        } else {
            needs = needRepository.findUnfulfilledByItemOrderByUrgency(donation.getItem().getId());
        }

        if (needs.isEmpty()) return null; // no match — donation stays OFFERED

        Need bestNeed = needs.get(0);
        int qty = Math.min(donation.getQuantity(), bestNeed.getQuantityNeeded());

        // Create the transfer
        Transfer transfer = new Transfer();
        transfer.setDonation(donation);
        transfer.setToOrganization(bestNeed.getOrganization());
        transfer.setQuantityAssigned(qty);
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setCreatedAt(LocalDateTime.now());
        transferRepository.save(transfer);

        // Mark donation as assigned
        donation.setStatus(DonationStatus.ASSIGNED);
        donationRepository.save(donation);

        // Fulfill the need if fully covered
        if (qty >= bestNeed.getQuantityNeeded()) {
            bestNeed.setFulfilled(true);
            needRepository.save(bestNeed);
        }

        System.out.printf("🔗 Auto-matched: %s → %s (%d units)%n",
                donation.getDonorName(),
                bestNeed.getOrganization().getName(),
                qty);

        // Notify donor that their donation was matched (ASSIGNED)
        emailService.sendDonationStatusEmail(donation);

        // Notify all active staff at the receiving organization
        List<String> staffEmails = userRepository
                .findByOrganizationId(bestNeed.getOrganization().getId())
                .stream()
                .filter(u -> u.getRole() == Role.STAFF || u.getRole() == Role.ADMIN)
                .map(u -> u.getEmail())
                .toList();
        emailService.sendStaffMatchNotification(staffEmails, transfer);

        return transfer;
    }
}
