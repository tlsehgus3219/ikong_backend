package com.ikongserver.repository;

import com.ikongserver.entity.GuardianInvitation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianInvitationRepository extends JpaRepository<GuardianInvitation, Long> {

    List<GuardianInvitation> findByPhoneAndStatus(String phone, String status);
}
