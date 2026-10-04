package com.fc.v2.model.custom;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 归层结论（判定那一处的回话：层、结论栏、界次代号、起用那一日都在这一张嘴上）
 *
 * @author fuce
 * @date 2026-10-04
 */
@ApiModel(value = "VehRunVerdict", description = "归层结论")
public class VehRunVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 所量的是车速还是连续驾驶时长 */
    @ApiModelProperty(value = "所量的是车速还是连续驾驶时长")
    private String measureKind;

    /** 车上报回来的数（报来那一头的单位） */
    @ApiModelProperty(value = "车上报回来的数（报来那一头的单位）")
    private BigDecimal reportValue;

    /** 报来那一头的单位 */
    @ApiModelProperty(value = "报来那一头的单位")
    private String reportUnit;

    /** 折到界册那一头后的数（取数、比数同一把舍法，屏上显的就是拿去比的那个数） */
    @ApiModelProperty(value = "折到界册那一头后的数")
    private BigDecimal convValue;

    /** 问话的那一刻 */
    @ApiModelProperty(value = "问话的那一刻")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date judgedAt;

    /** 归层 0够不上层 1提示 2严管 3立案 */
    @ApiModelProperty(value = "归层 0够不上层 1提示 2严管 3立案")
    private Integer levelNo;

    /** 结论栏：提示/严管/立案/未达起算/无可用界 */
    @ApiModelProperty(value = "结论栏：提示/严管/立案/未达起算/无可用界")
    private String conclusion;

    /** 界次代号（无可用界时留空，不许抄旁边那条的） */
    @ApiModelProperty(value = "界次代号（无可用界时留空）")
    private String ruleCode;

    /** 起用那一日（无可用界时留空） */
    @ApiModelProperty(value = "起用那一日（无可用界时留空）")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effStart;

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

    public BigDecimal getConvValue() {
        return convValue;
    }

    public void setConvValue(BigDecimal convValue) {
        this.convValue = convValue;
    }

    public Date getJudgedAt() {
        return judgedAt;
    }

    public void setJudgedAt(Date judgedAt) {
        this.judgedAt = judgedAt;
    }

    public Integer getLevelNo() {
        return levelNo;
    }

    public void setLevelNo(Integer levelNo) {
        this.levelNo = levelNo;
    }

    public String getConclusion() {
        return conclusion;
    }

    public void setConclusion(String conclusion) {
        this.conclusion = conclusion;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public Date getEffStart() {
        return effStart;
    }

    public void setEffStart(Date effStart) {
        this.effStart = effStart;
    }
}
