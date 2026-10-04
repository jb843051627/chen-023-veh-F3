package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 报警归层结论对象 t_veh_run_verdict
 *
 * @author fuce
 * @date 2026-10-04
 */
@TableName("t_veh_run_verdict")
@ApiModel(value = "TVehRunVerdict", description = "报警归层结论")
public class TVehRunVerdict implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 报警代号（结论挂在哪条报警上） */
    @TableField("alarm_no")
    @ApiModelProperty(value = "报警代号")
    private String alarmNo;

    /** 所量的是车速还是连续驾驶时长 */
    @TableField("measure_kind")
    @ApiModelProperty(value = "所量的是车速还是连续驾驶时长")
    private String measureKind;

    /** 车上报回来的数（报来那一头的单位） */
    @TableField("report_value")
    @ApiModelProperty(value = "车上报回来的数（报来那一头的单位）")
    private BigDecimal reportValue;

    /** 报来那一头的单位 */
    @TableField("report_unit")
    @ApiModelProperty(value = "报来那一头的单位")
    private String reportUnit;

    /** 折到界册那一头后的数（与比数同一把舍法） */
    @TableField("conv_value")
    @ApiModelProperty(value = "折到界册那一头后的数（与比数同一把舍法）")
    private BigDecimal convValue;

    /** 问话的那一刻 */
    @TableField("judged_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "问话的那一刻")
    private Date judgedAt;

    /** 归层 0够不上层 1提示 2严管 3立案 */
    @TableField("level_no")
    @ApiModelProperty(value = "归层 0够不上层 1提示 2严管 3立案")
    private Integer levelNo;

    /** 结论栏：提示/严管/立案/未达起算/无可用界 */
    @TableField("conclusion")
    @ApiModelProperty(value = "结论栏：提示/严管/立案/未达起算/无可用界")
    private String conclusion;

    /** 界次代号（无可用界时留空） */
    @TableField("rule_code")
    @ApiModelProperty(value = "界次代号（无可用界时留空）")
    private String ruleCode;

    /** 起用那一日（无可用界时留空） */
    @TableField("eff_start")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "起用那一日（无可用界时留空）")
    private Date effStart;

    /** 删除标记 0正常 1删除 */
    @TableField("del_flag")
    @ApiModelProperty(value = "删除标记 0正常 1删除")
    private Integer delFlag;

    /** 创建者 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建者")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    /** 更新者 */
    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    @ApiModelProperty(value = "更新者")
    private String updateBy;

    /** 更新时间 */
    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "更新时间")
    private Date updateTime;

    /** 备注 */
    @TableField("remark")
    @ApiModelProperty(value = "备注")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
