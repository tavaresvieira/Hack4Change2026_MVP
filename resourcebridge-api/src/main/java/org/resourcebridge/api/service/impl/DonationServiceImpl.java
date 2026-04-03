package org.resourcebridge.api.service.impl;

import lombok.RequiredArgsConstructor;
import org.resourcebridge.api.entity.Donation;
import org.resourcebridge.api.enums.DonationStatus;
import org.resourcebridge.api.exception.ResourceNotFoundException;
import org.resourcebridge.api.repository.DonationRepository;
import org.resourcebridge.api.service.DonationService;
import org.resourcebridge.api.service.EmailService;
import org.resourcebridge.api.service.MatchingService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final MatchingService matchingService;
    private final EmailService emailService;

    @Override
    public List<Donation> getAll() {
        return donationRepository.findAll();
    }

    @Override
    public Donation getById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Donation", id));
    }

    @Override
    public Donation save(Donation donation) {
        if (donation.getStatus() == null) {
            donation.setStatus(DonationStatus.OFFERED);
        }
        Donation saved = donationRepository.save(donation);

        // Auto-match to the highest-urgency need for this item
        if (saved.getStatus() == DonationStatus.OFFERED) {
            matchingService.autoMatch(saved);
            return donationRepository.findById(saved.getId()).orElse(saved);
        }

        return saved;
    }

    @Override
    public void deleteById(Long id) {
        donationRepository.deleteById(id);
    }

    @Override
    public List<Donation> findByStatus(DonationStatus status) {
        return donationRepository.findByStatus(status);
    }

    @Override
    public List<Donation> findByDonorEmail(String donorEmail) {
        return donationRepository.findByDonorEmail(donorEmail);
    }

    @Override
    public List<Donation> findAvailableByItem(Long itemId) {
        return donationRepository.findByItemIdAndStatus(itemId, DonationStatus.OFFERED);
    }

    @Override
    public Donation updateStatus(Long donationId, DonationStatus status) {
        Donation donation = getById(donationId);
        donation.setStatus(status);
        Donation saved = donationRepository.save(donation);

        // Notify donor asynchronously on meaningful status changes
        emailService.sendDonationStatusEmail(saved);

        return saved;
    }
}
