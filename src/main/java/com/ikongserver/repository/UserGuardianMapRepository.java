package com.ikongserver.repository;

import com.ikongserver.entity.Guardian;
import com.ikongserver.entity.UserGuardianMap;
import com.ikongserver.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserGuardianMapRepository extends JpaRepository<UserGuardianMap, Long> {

    List<UserGuardianMap> findByUser(Users user);

    List<UserGuardianMap> findByGuardian(Guardian guardian);

    long countByUserAndIsActive(Users user, String isActive);

    boolean existsByUserAndGuardian(Users user, Guardian guardian);

    // 보호자가 담당하는 활성 피보호자 매핑 목록
    List<UserGuardianMap> findByGuardianAndIsActive(Guardian guardian, String isActive);

    // 보호자 피보호자 연동 판별
    Optional<UserGuardianMap> findByUserIdAndGuardianId(Long userId, Long guardianId);
    boolean existsByUserIdAndGuardianId(Long userId, Long guardianId);

}
