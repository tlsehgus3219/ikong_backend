package com.ikongserver.repository;

import com.ikongserver.entity.Guardian;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    Optional<Guardian> findBySocialId(String socialId);
}
