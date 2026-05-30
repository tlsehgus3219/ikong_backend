package com.ikongserver.entity;

public enum ConditionType {

    //               hrWarnHigh, hrCritHigh, hrWarnLow, hrCritLow, brWarnHigh, brCritHigh, requireBoth
    HEART_ATTACK(   100,        120,        null,      null,      20,         25,         false), // 심근경색·허혈성 심장병 — 즉시 위험, 임계값만으로 판정
    HEART_FAILURE(  90,         110,        50,        45,        20,         25,         true),  // 심부전 — 기준선 병행 확인
    PNEUMONIA(      90,         110,        null,      null,      22,         26,         true),  // 폐렴 — 기준선 병행 확인
    STROKE(         110,        120,        45,        40,        20,         25,         false), // 뇌졸중 — 즉시 위험, 임계값만으로 판정
    COPD(           null,       null,       null,      null,      20,         25,         true),  // 만성폐쇄성폐질환 — 기준선 병행 확인 (HR은 베이스라인 비율 적용)
    SEPSIS(         90,         110,        null,      null,      22,         26,         false), // 패혈증 — 즉시 위험, 임계값만으로 판정
    ARRHYTHMIA(     130,        150,        45,        40,        20,         25,         true);  // 부정맥 — 기준선 병행 확인

    public final Integer hrWarnHigh;
    public final Integer hrCritHigh;
    public final Integer hrWarnLow;
    public final Integer hrCritLow;
    public final Integer brWarnHigh;
    public final Integer brCritHigh;
    public final boolean requireBoth; // true: 질병 임계값 AND 기준선 둘 다 초과해야 이상, false: 임계값만 초과해도 이상

    ConditionType(Integer hrWarnHigh, Integer hrCritHigh,
                  Integer hrWarnLow, Integer hrCritLow,
                  Integer brWarnHigh, Integer brCritHigh,
                  boolean requireBoth) {
        this.hrWarnHigh  = hrWarnHigh;
        this.hrCritHigh  = hrCritHigh;
        this.hrWarnLow   = hrWarnLow;
        this.hrCritLow   = hrCritLow;
        this.brWarnHigh  = brWarnHigh;
        this.brCritHigh  = brCritHigh;
        this.requireBoth = requireBoth;
    }
}
