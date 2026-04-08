package com.ikongserver.repository;

import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGuardianMapRepository extends JpaRepository<UserGuardianMap, Long> {

    List<UserGuardianMap> findByUser(Users user);

    long countByUserAndIsActive(Users user, String isActive);
}
