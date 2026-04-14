package com.ikongserver.repository;

import com.ikongserver.entity.LogoutToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogoutTokenRepository extends JpaRepository<LogoutToken, Long> {
    boolean existsByToken(String token);
}
