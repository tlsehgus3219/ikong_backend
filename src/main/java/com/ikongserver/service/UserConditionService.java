package com.ikongserver.service;

import com.ikongserver.dto.UserConditionDto.ConditionsResponse;
import com.ikongserver.dto.UserConditionDto.UpdateConditionsRequest;
import com.ikongserver.entity.ConditionType;
import com.ikongserver.entity.UserCondition;
import com.ikongserver.entity.Users;
import com.ikongserver.repository.UserConditionRepository;
import com.ikongserver.repository.UsersRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserConditionService {

    private final UserConditionRepository userConditionRepository;
    private final UsersRepository usersRepository;

    // 현재 등록된 질환 목록 조회
    @Transactional(readOnly = true)
    public ConditionsResponse getConditions(Long userId) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<ConditionType> conditions = userConditionRepository.findByUser(user).stream()
            .map(UserCondition::getConditionType)
            .toList();

        return new ConditionsResponse(conditions);
    }

    // 질환 목록 전체 교체 — 기존 목록 삭제 후 새 목록으로 저장
    // conditions가 빈 리스트이면 전체 삭제 (질환 없음 상태로 복귀)
    @Transactional
    public ConditionsResponse updateConditions(Long userId, UpdateConditionsRequest request) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userConditionRepository.deleteByUser(user);

        List<ConditionType> result = List.of();
        if (request.conditions() != null && !request.conditions().isEmpty()) {
            List<UserCondition> newConditions = request.conditions().stream()
                .distinct()
                .map(type -> UserCondition.builder().user(user).conditionType(type).build())
                .toList();
            userConditionRepository.saveAll(newConditions);
            result = request.conditions().stream().distinct().toList();
        }

        return new ConditionsResponse(result);
    }

    // userId로 등록된 ConditionType 목록 반환 — EmergencyEventService에서 임계값 계산 시 사용
    @Transactional(readOnly = true)
    public List<ConditionType> getConditionTypes(Long userId) {
        Users user = usersRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return userConditionRepository.findByUser(user).stream()
            .map(UserCondition::getConditionType)
            .toList();
    }
}
