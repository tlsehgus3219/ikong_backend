package com.ikongserver.repository;

import com.ikongserver.entity.Users;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsersRepository extends JpaRepository<Users, Long> {
    Optional<Users> findBySocialId(String socialId);
}
