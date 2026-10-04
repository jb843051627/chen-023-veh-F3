package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TVehRunVerdictMapper;
import com.fc.v2.model.auto.TVehRunVerdict;
import com.fc.v2.model.custom.VehRunVerdict;
import com.fc.v2.service.ITVehRunLineService;
import com.fc.v2.service.ITVehRunVerdictService;
import com.fc.v2.util.StringUtils;

/**
 * 报警归层结论 Service业务层处理。
 *
 * 判定四问（哪条界说话、够不够得上层、压线归哪层、报来的数怎么折）
 * 全在界次判定那一处作答，这里一字不改照写落库。
 *
 * @author fuce
 * @date 2026-10-04
 */
@Service
public class TVehRunVerdictServiceImpl implements ITVehRunVerdictService {

    @javax.annotation.Resource
    private TVehRunVerdictMapper vehRunVerdictMapper;

    @javax.annotation.Resource
    private ITVehRunLineService vehRunLineService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TVehRunVerdict judgeAndStore(String alarmNo, String measureKind, BigDecimal reportValue,
                                        String reportUnit, Date judgedAt) {
        if (StringUtils.isEmpty(alarmNo)) {
            throw new IllegalArgumentException("报警代号空着，结论没处挂");
        }
        // 归层只认判定那一处的回话；材料不全那一处直接拒绝，这里不落半成品
        VehRunVerdict v = vehRunLineService.judge(measureKind, reportValue, reportUnit, judgedAt);
        TVehRunVerdict row = new TVehRunVerdict();
        row.setAlarmNo(alarmNo);
        row.setMeasureKind(v.getMeasureKind());
        row.setReportValue(v.getReportValue());
        row.setReportUnit(v.getReportUnit());
        row.setConvValue(v.getConvValue());
        row.setJudgedAt(v.getJudgedAt());
        row.setLevelNo(v.getLevelNo());
        row.setConclusion(v.getConclusion());
        // 界次代号、起用那一日照判定那一处说的话写；无可用界时两栏留空，不许抄旁边那条
        row.setRuleCode(v.getRuleCode());
        row.setEffStart(v.getEffStart());
        row.setDelFlag(0);
        vehRunVerdictMapper.insert(row);
        return row;
    }

    @Override
    public List<TVehRunVerdict> selectTVehRunVerdictList(QueryWrapper<TVehRunVerdict> queryWrapper) {
        return this.vehRunVerdictMapper.selectList(queryWrapper);
    }

    @Override
    public List<TVehRunVerdict> listDangling() {
        List<TVehRunVerdict> all = this.vehRunVerdictMapper.selectList(new QueryWrapper<TVehRunVerdict>()
                .eq("del_flag", 0)
                .orderByAsc("judged_at")
                .orderByAsc("id"));
        List<TVehRunVerdict> diff = new ArrayList<TVehRunVerdict>();
        for (TVehRunVerdict row : all) {
            // 屏上没挂界次代号的（无可用界），不在这条对账线上
            if (StringUtils.isEmpty(row.getRuleCode())) {
                continue;
            }
            if (stale(row)) {
                diff.add(row);
            }
        }
        return diff;
    }

    /**
     * 拿单子上问话的那一刻回判定那一处再问一遍：
     * 如今请不出在位界、或请出的界（界次代号、起用那一日）与单子上挂的不是同一条，即算差开。
     */
    private boolean stale(TVehRunVerdict row) {
        VehRunVerdict now;
        try {
            now = vehRunLineService.judge(row.getMeasureKind(), row.getReportValue(),
                    row.getReportUnit(), row.getJudgedAt());
        } catch (IllegalArgumentException e) {
            // 单子上的材料如今连问话都问不成，同样算差开
            return true;
        }
        if (now.getRuleCode() == null) {
            return true;
        }
        if (!row.getRuleCode().equals(now.getRuleCode())) {
            return true;
        }
        Date a = row.getEffStart();
        Date b = now.getEffStart();
        return a == null ? b != null : !a.equals(b);
    }
}
