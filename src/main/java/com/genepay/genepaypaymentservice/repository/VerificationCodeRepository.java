package com.genepay.genepaypaymentservice.repository;

import com.genepay.genepaypaymentservice.model.VerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findByEmailAndType(String email, VerificationCode.VerificationType type);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.expiryTime < :now")
    void deleteExpiredCodes(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM VerificationCode v WHERE v.email = :email AND v.type = :type")
    void deleteByEmailAndType(String email, VerificationCode.VerificationType type);

    boolean existsByEmailAndType(String email, VerificationCode.VerificationType type);
}
