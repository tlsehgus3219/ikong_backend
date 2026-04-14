package com.ikongserver.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "user_setting")
public class UserSetting {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    private int hrMax;
    private int hrMin;
    private int inactivityLimit;

    public void updateHr (int hrMax, int hrMin) {
        this.hrMax = hrMax;
        this.hrMin = hrMin;
    }

    public void updateInactivityLimit(int inactivityLimit) {
        this.inactivityLimit = inactivityLimit;
    }

    @Builder
    public UserSetting (Users user, int hrMax, int hrMin, int inactivityLimit) {
        this.user = user;
        this.hrMax = hrMax;
        this.hrMin = hrMin;
        this.inactivityLimit = inactivityLimit;
    }




}
