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
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id")
    private EmergencyEvent emergencyEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    private String message;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime sentAt;

    private String readYN = "N";

    public void updateReadYN(String readYN) {
        this.readYN = readYN;
    }

    @Builder
    public Notification(EmergencyEvent emergencyEvent, Guardian guardian, String message) {
        this.emergencyEvent = emergencyEvent;
        this.guardian = guardian;
        this.message = message;
    }

}
