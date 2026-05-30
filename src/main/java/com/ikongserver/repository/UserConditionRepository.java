package com.ikongserver.repository;

import com.ikongserver.entity.ConditionType;
import com.ikongserver.entity.UserCondition;
import com.ikongserver.entity.Users;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserConditionRepository extends JpaRepository<UserCondition, Long> {

    List<UserCondition> findByUser(Users user);

    void deleteByUserAndConditionType(Users user, ConditionType conditionType);

    void deleteByUser(Users user);

    boolean existsByUserAndConditionType(Users user, ConditionType conditionType);
}
