package com.fc.v2.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 归层问话材料（只收材料：层、结论栏、界次代号、起用那一日不许递，判定那一处作答后才落库）
 *
 * @author fuce
 * @date 2026-10-04
 */
@ApiModel(value = "VehRunVerdictAsk", description = "归层问话材料")
public class VehRunVerdictAsk implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 报警代号（结论挂在哪条报警上） */
    @ApiModelProperty(value = "报警代号")
    private String alarmNo;

    /** 所量的是车速还是连续驾驶时长（speed/duration） */
    @ApiModelProperty(value = "所量的是车速还是连续驾驶时长（speed/duration）")
    private String measureKind;

    /** 车上报回来的数（报来那一头的单位：车速 m/s、时长 min；已是界册单位的不再折） */
    @ApiModelProperty(value = "车上报回来的数")
    private BigDecimal reportValue;

    /** 报来那一头的单位（m/s、min、km/h、h） */
    @ApiModelProperty(value = "报来那一头的单位")
    private String reportUnit;

    /** 问话那一刻（报警发生的那一刻；补判旧件就递旧件自己的那一刻） */
    @ApiModelProperty(value = "问话那一刻")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date judgedAt;

    public String getAlarmNo() {
        return alarmNo;
    }

    public void setAlarmNo(String alarmNo) {
        this.alarmNo = alarmNo;
    }

    public String getMeasureKind() {
        return measureKind;
    }

    public void setMeasureKind(String measureKind) {
        this.measureKind = measureKind;
    }

    public BigDecimal getReportValue() {
        return reportValue;
    }

    public void setReportValue(BigDecimal reportValue) {
        this.reportValue = reportValue;
    }

    public String getReportUnit() {
        return reportUnit;
    }

    public void setReportUnit(String reportUnit) {
        this.reportUnit = reportUnit;
    }

    public Date getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt(Date judgedAt) {
        this.judgedAt = judgedAt;
    }
}
