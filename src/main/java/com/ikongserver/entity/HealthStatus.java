package com.ikongserver.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "health_stats")
public class HealthStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    private LocalDate statDate;

    private int avgHeartRate;
    private int movementCount;
    private double sleepHours;

    @Builder
    public HealthStatus(Users user, LocalDate statDate, int avgHeartRate, int movementCount,
        int sleepHours) {
        this.user = user;
        this.statDate = statDate;
        this.avgHeartRate = avgHeartRate;
        this.movementCount = movementCount;
        this.sleepHours = sleepHours;
    }


}
