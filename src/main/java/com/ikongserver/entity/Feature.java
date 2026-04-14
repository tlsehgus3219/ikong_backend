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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "feature")
public class Feature {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "feature_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    private double avgHeartRate;
    private double hrVariation;
    private double brVariation;
    private int noMoveSec;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    public void updateAvgHeartRate(double avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }

    public void updateHrVariation(double hrVariation) {
        this.hrVariation = hrVariation;
    }

    public void updateBrVariation(double brVariation) {
        this.brVariation = brVariation;
    }

    public void updateNoMoveSec(int noMoveSec) {
        this.noMoveSec = noMoveSec;
    }

    @Builder
    public Feature (Users user, Device device, double avgHeartRate, double hrVariation, double brVariation, int noMoveSec) {
        this.user = user;
        this.device = device;
        this.avgHeartRate = avgHeartRate;
        this.hrVariation = hrVariation;
        this.brVariation = brVariation;
        this.noMoveSec = noMoveSec;
    }



}
