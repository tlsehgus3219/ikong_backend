package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
@Table(name = "vital", indexes = {
    @Index(name = "idx_vital_device_time", columnList = "device_id, recorded_at DESC")
})
public class Vital {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "vital_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    private int heartRate;
    private int breathRate;
    private int movement;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime recordedAt;


    @Builder
    public Vital (Users user, Device device, int heartRate, int breathRate, int movement) {
        this.user = user;
        this.device = device;
        this.heartRate = heartRate;
        this.breathRate = breathRate;
        this.movement = movement;
    }

}
