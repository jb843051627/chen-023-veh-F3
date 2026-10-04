package com.fc.v2.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.mapper.auto.TVehRunLineMapper;
import com.fc.v2.model.auto.TVehRunLine;
import com.fc.v2.model.custom.VehRunVerdict;
import com.fc.v2.service.ITVehRunLineService;
import com.fc.v2.util.StringUtils;

/**
 * 运行界次判定线 Service业务层处理。
 *
 * 归层只有这一套走法：取数、折算、比数、落层全在这一处。
 * 界册就那几条界，逐条走一遍在内存里做，不去 SQL 里拼墙钟串——
 * JDBC 的 serverTimezone 与本机时区不对称，拼串会在"切换当日"这类用例上随机错档；
 * 且 listAvailable/usable/countAvailable/judge 天然同踩一条道，两个位置问同一刻回同一句话。
 *
 * @author fuce
 * @date 2026-09-14
 */
@Service
public class TVehRunLineServiceImpl implements ITVehRunLineService {

    /** 界册分界数的精度（界册 decimal(12,3)），折算后的报数也落到这一档 */
    private static final int SCALE = 3;

    /**
     * 同一把舍法：四舍五入。取数是一处、比数是一处，两边只许用这一把——
     * 去年冬季那条压线的报警，就是一处四舍五入、一处直接截尾，生生差了一层。
     */
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /** 每秒多少米 → 每小时多少公里 */
    private static final BigDecimal MS_TO_KMH = new BigDecimal("3.6");

    /** 分钟 → 小时 */
    private static final BigDecimal MIN_TO_HOUR = new BigDecimal("60");

    /**
     * 多条在位听谁的：先比交班次序，序高的那条说话；序相同再比界次代号，
     * 照排法落在末位的胜出；再同按主键定死——同一刻问出两个答复即算错。
     */
    private static final Comparator<TVehRunLine> WINNER_ORDER = new Comparator<TVehRunLine>() {
        @Override
        public int compare(TVehRunLine a, TVehRunLine b) {
            int pa = a.getPriority() == null ? 0 : a.getPriority();
            int pb = b.getPriority() == null ? 0 : b.getPriority();
            if (pa != pb) {
                return pb - pa;
            }
            String ca = a.getRuleCode() == null ? "" : a.getRuleCode();
            String cb = b.getRuleCode() == null ? "" : b.getRuleCode();
            int byCode = cb.compareTo(ca);
            if (byCode != 0) {
                return byCode;
            }
            long ia = a.getId() == null ? 0L : a.getId();
            long ib = b.getId() == null ? 0L : b.getId();
            return Long.compare(ib, ia);
        }
    };

    @javax.annotation.Resource
    private TVehRunLineMapper vehRunLineMapper;

    @Override
    public TVehRunLine selectTVehRunLineById(Long id) {
        return this.vehRunLineMapper.selectById(id);
    }

    @Override
    public List<TVehRunLine> selectTVehRunLineList(QueryWrapper<TVehRunLine> queryWrapper) {
        return this.vehRunLineMapper.selectList(queryWrapper);
    }

    @Override
    public VehRunVerdict judge(String measureKind, BigDecimal reportValue, String reportUnit, Date at) {
        // 材料不全没法问话：所量口径、报来的数、报来单位、问话那一刻，缺任一个都不开腔
        if (StringUtils.isEmpty(measureKind) || reportValue == null
                || StringUtils.isEmpty(reportUnit) || at == null) {
            throw new IllegalArgumentException("所量口径、报来的数、报来单位、问话那一刻，缺任一个都没法问话");
        }
        if (reportValue.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("报来的数不能是负的");
        }
        // 同一刻、同一口径只许一条界说话：名单排定后取头一条，名单与屏上那份同出一处
        TVehRunLine winner = null;
        for (TVehRunLine r : listAvailable(at)) {
            if (measureKind.equals(r.getMeasureKind())) {
                winner = r;
                break;
            }
        }
        VehRunVerdict verdict = new VehRunVerdict();
        verdict.setMeasureKind(measureKind);
        verdict.setReportValue(reportValue);
        verdict.setReportUnit(reportUnit);
        verdict.setJudgedAt(at);
        if (winner == null) {
            // 一条界都够不上：不给层，界次代号与起用那一日两栏一并留空，
            // 不许抄旁边那条的数字凑一个看着顺眼的答复
            verdict.setLevelNo(LEVEL_NONE);
            verdict.setConclusion(CONCLUSION_NO_LINE);
            return verdict;
        }
        // 先折单位再比数：只往界册那一头折，起算、够线、封顶三头用同一个折好的数
        BigDecimal conv = convert(reportValue, reportUnit, winner.getUnitKind());
        verdict.setConvValue(conv);
        verdict.setLevelNo(levelOf(winner, conv));
        verdict.setConclusion(conclusionOf(verdict.getLevelNo()));
        verdict.setRuleCode(winner.getRuleCode());
        verdict.setEffStart(winner.getEffStart());
        return verdict;
    }

    @Override
    public int evaluate(String ruleCode, BigDecimal input, Date at) {
        if (StringUtils.isEmpty(ruleCode) || input == null || at == null
                || input.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        // 旧办法凭界次代号取值：数已是界册那一头的单位，落层与 judge 同一处
        for (TVehRunLine r : listAvailable(at)) {
            if (ruleCode.equals(r.getRuleCode())) {
                return levelOf(r, input.setScale(SCALE, ROUNDING));
            }
        }
        return 0;
    }

    @Override
    public int evaluateTop(BigDecimal input, Date at) {
        if (input == null || at == null || input.compareTo(BigDecimal.ZERO) < 0) {
            return 0;
        }
        List<TVehRunLine> avail = listAvailable(at);
        if (avail.isEmpty()) {
            return 0;
        }
        return levelOf(avail.get(0), input.setScale(SCALE, ROUNDING));
    }

    @Override
    public List<TVehRunLine> listAvailable(Date at) {
        if (at == null) {
            return new ArrayList<TVehRunLine>();
        }
        List<TVehRunLine> all = this.vehRunLineMapper.selectList(
                new QueryWrapper<TVehRunLine>().eq("del_flag", 0));
        List<TVehRunLine> hit = new ArrayList<TVehRunLine>();
        for (TVehRunLine r : all) {
            if (speakable(r, at)) {
                hit.add(r);
            }
        }
        hit.sort(WINNER_ORDER);
        return hit;
    }

    @Override
    public boolean usable(Long id, Date at) {
        if (id == null || at == null) {
            return false;
        }
        TVehRunLine r = this.vehRunLineMapper.selectById(id);
        return r != null && r.getDelFlag() != null && r.getDelFlag() == 0 && speakable(r, at);
    }

    @Override
    public int countAvailable(Date at) {
        return listAvailable(at).size();
    }

    @Override
    public List<String> checkLine(Long id) {
        if (id == null) {
            return Collections.singletonList("界次主键空着，没法查这条界");
        }
        TVehRunLine r = this.vehRunLineMapper.selectById(id);
        if (r == null) {
            return Collections.singletonList("册里翻不着这条界");
        }
        return lineProblems(r);
    }

    /**
     * 这条界这一刻开不开口：在位（起用那一日已经够着、交班那一日还没到）且界线站得住。
     * 交班一栏空着的存量界认作一直在位；已停用的不开口。
     */
    private boolean speakable(TVehRunLine r, Date at) {
        return inEffect(r, at) && lineProblems(r).isEmpty();
    }

    /**
     * 在位只认一套说法：起用那一日已经够着、交班那一日还没到（交班当日不含），
     * 旧界管到交班日之前、新界从交班日起头；交班空着的认作一直在位；
     * 起用那一日空着的，够不着起头，算不上在位。
     */
    private boolean inEffect(TVehRunLine r, Date at) {
        if (r.getStatus() == null || r.getStatus() != LINE_ACTIVE) {
            return false;
        }
        if (r.getEffStart() == null || at.before(r.getEffStart())) {
            return false;
        }
        return r.getEffEnd() == null || at.before(r.getEffEnd());
    }

    /**
     * 界线站不站得住：三个分界数缺一个立不起来，后一个不比前一个大同样立不起来，
     * 界册单位空着的没法折算、一样开不了口。
     * 回话指名哪一格空着、哪一格没越过前头那一格，不许只讲"数值不对劲"。
     */
    private List<String> lineProblems(TVehRunLine r) {
        List<String> problems = new ArrayList<String>();
        if (StringUtils.isEmpty(r.getUnitKind())) {
            problems.add("界册单位(unit_kind)空着");
        }
        if (r.getTh1Max() == null) {
            problems.add("起算数值(th1_max)空着");
        }
        if (r.getTh2Max() == null) {
            problems.add("够线数值(th2_max)空着");
        }
        if (r.getTh3Max() == null) {
            problems.add("封顶数值(th3_max)空着");
        }
        if (!problems.isEmpty()) {
            return problems;
        }
        if (r.getTh2Max().compareTo(r.getTh1Max()) <= 0) {
            problems.add("够线数值(th2_max)没越过起算数值(th1_max)");
        }
        if (r.getTh3Max().compareTo(r.getTh2Max()) <= 0) {
            problems.add("封顶数值(th3_max)没越过够线数值(th2_max)");
        }
        return problems;
    }

    /**
     * 折算只往界册那一头折：报来的数折成界册单位再比，够线那一头与封顶那一头同样折。
     * 取数、比数两边同走这一处、同一把舍法；报来的已是界册单位的，只过同一把舍法不再折。
     */
    private BigDecimal convert(BigDecimal value, String fromUnit, String toUnit) {
        if (fromUnit.equals(toUnit)) {
            return value.setScale(SCALE, ROUNDING);
        }
        if (UNIT_M_PER_SEC.equals(fromUnit) && UNIT_KM_PER_HOUR.equals(toUnit)) {
            return value.multiply(MS_TO_KMH).setScale(SCALE, ROUNDING);
        }
        if (UNIT_MINUTE.equals(fromUnit) && UNIT_HOUR.equals(toUnit)) {
            return value.divide(MIN_TO_HOUR, SCALE, ROUNDING);
        }
        throw new IllegalArgumentException(
                "报来单位(" + fromUnit + ")与界册单位(" + toUnit + ")对不上，没法折算");
    }

    /**
     * 落层只有这一套：够着起算不到够线是提示，够着够线不到封顶是严管，够着封顶是立案；
     * 恰好压在某个分界数上的归上一层（起算、够线、封顶，够着即入），两个位置问出来落在同一层；
     * 不够起算的够不上层。
     */
    private int levelOf(TVehRunLine rule, BigDecimal conv) {
        if (conv.compareTo(rule.getTh1Max()) < 0) {
            return LEVEL_NONE;
        }
        if (conv.compareTo(rule.getTh2Max()) < 0) {
            return LEVEL_HINT;
        }
        if (conv.compareTo(rule.getTh3Max()) < 0) {
            return LEVEL_STRICT;
        }
        return LEVEL_FILE;
    }

    /** 结论栏写法与层一一对应，不许另造字样 */
    private static String conclusionOf(int level) {
        switch (level) {
            case LEVEL_HINT:
                return CONCLUSION_HINT;
            case LEVEL_STRICT:
                return CONCLUSION_STRICT;
            case LEVEL_FILE:
                return CONCLUSION_FILE;
            default:
                return CONCLUSION_BELOW_START;
        }
    }
}
