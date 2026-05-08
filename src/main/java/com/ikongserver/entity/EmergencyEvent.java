package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "emergency_event")
public class EmergencyEvent {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "event_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    private String eventType;
    private String status;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void updateStatus(String status) {
        this.status = status;
    }

    public String getEventDescription() {
        return switch (this.eventType) {
            case "FALL" -> "낙상이 감지되었습니다.";
            case "HEART_ISSUE" -> "심박수 이상이 감지되었습니다.";
            case "BREATH_ISSUE" -> "호흡수 이상이 감지되었습니다.";
            default -> "이상이 감지되었습니다.";
        };
    }

    @Builder
    public EmergencyEvent(Users user, Device device,String eventType, String status) {
        this.user = user;
        this.device = device;
        this.eventType = eventType;
        this.status = status;
    }

}
