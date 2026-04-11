package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
@Table(name = "ai_result")
public class AiResult {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "ai_result_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", unique = true)
    private Feature feature;

    private String resultType;
    private double confidence;
    private String modelVer;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AiResult(Users user, Feature feature, String resultType, double confidence, String modelVer) {
        this.user = user;
        this.feature = feature;
        this.resultType = resultType;
        this.confidence = confidence;
        this.modelVer = modelVer;
    }

}
