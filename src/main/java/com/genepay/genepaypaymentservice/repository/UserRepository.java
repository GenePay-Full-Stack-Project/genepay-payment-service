package com.genepay.genepaypaymentservice.repository;

import com.genepay.genepaypaymentservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByNicNumber(String nicNumber);

    Optional<User> findByFaceId(String faceId);

    Optional<User> findByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByFaceId(String faceId);

    boolean existsByNicNumber(String nicNumber);

}
