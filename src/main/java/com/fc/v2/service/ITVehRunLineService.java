package com.fc.v2.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.model.auto.TVehRunLine;
import com.fc.v2.model.custom.VehRunVerdict;

/**
 * 运行界次判定线 Service接口。
 *
 * 哪条界说话、够不够得上层、压线归哪层、报来的数怎么折——
 * 这四问只由这一处作答，别处不许再辟一个能出这个结果的入口。
 *
 * @author fuce
 * @date 2026-09-14
 */
public interface ITVehRunLineService {

    /** 层：够不上层（未达起算，或一条界都请不出） */
    int LEVEL_NONE = 0;
    /** 层：提示 */
    int LEVEL_HINT = 1;
    /** 层：严管 */
    int LEVEL_STRICT = 2;
    /** 层：立案 */
    int LEVEL_FILE = 3;

    /** 结论栏写法：提示 */
    String CONCLUSION_HINT = "提示";
    /** 结论栏写法：严管 */
    String CONCLUSION_STRICT = "严管";
    /** 结论栏写法：立案 */
    String CONCLUSION_FILE = "立案";
    /** 结论栏写法：未达起算（界请得出，数够不上起算） */
    String CONCLUSION_BELOW_START = "未达起算";
    /** 结论栏写法：无可用界（一条界都请不出，界次代号与起用那一日两栏一并留空） */
    String CONCLUSION_NO_LINE = "无可用界";

    /** 界的情形：在位 */
    int LINE_ACTIVE = 0;
    /** 界的情形：已停用（册里翻得着、屏上列得出，只是不许捞它去比数） */
    int LINE_DISABLED = 1;

    /** 所量的：车速 */
    String MEASURE_SPEED = "speed";
    /** 所量的：连续驾驶时长 */
    String MEASURE_DURATION = "duration";

    /** 界册单位：车速记每小时多少公里 */
    String UNIT_KM_PER_HOUR = "km/h";
    /** 界册单位：连续驾驶时长记小时 */
    String UNIT_HOUR = "h";
    /** 报来单位：车速给成每秒多少米 */
    String UNIT_M_PER_SEC = "m/s";
    /** 报来单位：时长给成多少分钟 */
    String UNIT_MINUTE = "min";

    /** 按主键查询规则 */
    TVehRunLine selectTVehRunLineById(Long id);

    /** 界册名单（屏上列得出：含已停用、剔删除标记），分页用 */
    List<TVehRunLine> selectTVehRunLineList(QueryWrapper<TVehRunLine> queryWrapper);

    /**
     * 归层判定（另开的这条道，名目在此）：报来的数先折到界册那一头再比，
     * 取数、比数同一把舍法；问话那一刻哪条界在位、多条在位听谁的、
     * 压线归哪一层，全在这一处定，屏上不许另摆第二套。
     * 材料不全（所量口径、报来的数、报来单位、问话那一刻缺任一个）直接拒绝问话。
     */
    VehRunVerdict judge(String measureKind, BigDecimal reportValue, String reportUnit, Date at);

    /**
     * 旧办法：凭界次代号取值，数按界册那一头的单位递进来。
     * 签名照原样摆着、不挂新参数；落层口径与 judge 同一处（1提示 2严管 3立案），
     * 请不出在位界或数够不上起算返回 0。
     */
    int evaluate(String ruleCode, BigDecimal input, Date at);

    /** 旧办法：at 时刻在位界中交班次序最高者的层（数按界册单位递进来）；无在位界返回 0 */
    int evaluateTop(BigDecimal input, Date at);

    /**
     * at 时刻在位名单（交班次序降序、界次代号降序）。
     * 屏上那份名单与判定逐条走一遍凑出来的名单，出处同是这一处。
     */
    List<TVehRunLine> listAvailable(Date at);

    /** 单条界在 at 时刻开不开口（口径与 listAvailable 逐条一致） */
    boolean usable(Long id, Date at);

    /** at 时刻在位界条数（与 listAvailable 同一处点算） */
    int countAvailable(Date at);

    /**
     * 这条界线站不站得住：三个分界数缺一个、或后一个没越过前一个，都立不起来。
     * 回话指名哪一格空着、哪一格没越过前头那一格；返回空单子表示站得住。
     */
    List<String> checkLine(Long id);
}
