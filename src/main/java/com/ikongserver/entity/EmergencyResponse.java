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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "emergency_response")
public class EmergencyResponse {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "emergency_response_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private EmergencyEvent emergencyEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_guardian")
    private Guardian guardian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responded_by_user")
    private Users user;

    private String responseType;


    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime respondedAt;

    public void updateResponseType(String responseType) {
        this.responseType = responseType;
    }

    @Builder
    public EmergencyResponse(EmergencyEvent emergencyEvent, Guardian guardian, Users user,
        String responseType) {
        this.emergencyEvent = emergencyEvent;
        this.guardian = guardian;
        this.user = user;
        this.responseType = responseType;
    }
}
