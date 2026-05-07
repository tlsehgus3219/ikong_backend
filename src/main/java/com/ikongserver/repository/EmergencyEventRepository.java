package com.ikongserver.repository;

import com.ikongserver.entity.EmergencyEvent;
import com.ikongserver.entity.Users;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyEventRepository extends JpaRepository<EmergencyEvent, Long> {

    // 특정 사용자에게 해당 상태의 이벤트가 존재하는지 확인 (중복 알림 방지용)
    boolean existsByUserAndStatus(Users user, String status);

    // 특정 사용자의 가장 최근 미해결 이벤트 1건 조회 (피보호자 앱 화면 표시용)
    Optional<EmergencyEvent> findTopByUserAndStatusOrderByCreatedAtDesc(Users user, String status);

    // 보호자가 담당하는 여러 피보호자의 상태별 이벤트 수 집계 (요약 화면용)
    long countByUserInAndStatus(List<Users> users, String status);

    // 보호자가 담당하는 여러 피보호자의 전체 이벤트 목록 조회, 최신순 정렬
    List<EmergencyEvent> findByUserInOrderByCreatedAtDesc(List<Users> users);

    // 보호자가 담당하는 여러 피보호자 중 특정 상태(PENDING/RESOLVED)의 이벤트 목록 조회
    List<EmergencyEvent> findByUserInAndStatus(List<Users> users, String status);

    // 단일 사용자의 상태별 이벤트 수 집계
    long countByUserAndStatus(Users user, String status);
}
