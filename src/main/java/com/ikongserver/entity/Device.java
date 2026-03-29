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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "device")
public class Device {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "device_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    private String deviceType;

    @Column(unique = true)
    private String serialNum;
    private String locationNm;

    public void updateLocationNm(String locationNm) {
        this.locationNm = locationNm;
    }

    @Builder
    public Device(Users user, String deviceType, String serialNum, String locationNm) {
        this.user = user;
        this.deviceType = deviceType;
        this.serialNum = serialNum;
        this.locationNm = locationNm;
    }

}
