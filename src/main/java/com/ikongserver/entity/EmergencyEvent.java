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
    private LocalDateTime detectedAt;

    public void updateStatus(String status) {
        this.status = status;
    }

    @Builder
    public EmergencyEvent(Users user, Device device, String eventType, String status) {
        this.user = user;
        this.device = device;
        this.eventType = eventType;
        this.status = status;
    }

}
