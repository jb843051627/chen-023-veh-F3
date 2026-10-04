package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TVehRunVerdict;

/**
 * 报警归层结论 Service接口。
 *
 * 结论只许从判定那一处走出来：这里只收问话材料，
 * 层、结论栏、界次代号、起用那一日全照判定那一处说的话落库，
 * 不许谁先把结果填上、事后再补材料。
 *
 * @author fuce
 * @date 2026-10-04
 */
public interface ITVehRunVerdictService {

    /**
     * 归层落结论（唯一落库口）：递问话材料（报警代号、所量口径、报来的数、
     * 报来单位、问话那一刻），层与界次栏位由判定那一处作答后一并入库。
     * 材料不全直接拒绝，不落半成品结论。
     */
    TVehRunVerdict judgeAndStore(String alarmNo, String measureKind, BigDecimal reportValue,
                                 String reportUnit, Date judgedAt);

    /** 结论名单（屏上那份，分页用） */
    List<TVehRunVerdict> selectTVehRunVerdictList(QueryWrapper<TVehRunVerdict> queryWrapper);

    /**
     * 对账：屏上仍挂着界次代号、册里那一刻却请不出在位界的单子，一条条指出来。
     * 逐条拿单子上问话的那一刻回判定那一处再问一遍，
     * 如今请不出界、或请出的界与单子上挂的不是同一条的，就是差开的单子。
     */
    List<TVehRunVerdict> listDangling();
}
