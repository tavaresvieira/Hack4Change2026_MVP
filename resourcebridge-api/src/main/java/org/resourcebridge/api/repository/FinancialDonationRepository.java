package org.resourcebridge.api.repository;

import org.resourcebridge.api.entity.FinancialDonation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface FinancialDonationRepository extends JpaRepository<FinancialDonation, Long> {

    @Query("SELECT COALESCE(SUM(f.amount), 0) FROM FinancialDonation f")
    BigDecimal sumAllAmounts();
}
