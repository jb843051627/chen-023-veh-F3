package com.fc.v2;

import com.fc.v2.mapper.auto.TVehRunLineMapper;
import com.fc.v2.model.auto.TVehRunLine;
import com.fc.v2.model.custom.VehRunVerdict;
import com.fc.v2.service.ITVehRunLineService;
import com.fc.v2.service.impl.TVehRunLineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 归层判定那一处的口径测试：折算与舍法、在位窗口、多条在位、压线归层、
 * 无可用界、已停用不开口、界线站不站得住——两个位置问同一刻得回同一句话。
 */
class VehRunLineJudgeTest {

    private List<TVehRunLine> registry;
    private TVehRunLineServiceImpl service;

    @BeforeEach
    void setUp() {
        registry = new ArrayList<>();
        TVehRunLineMapper mapper = mock(TVehRunLineMapper.class);
        // 模拟 SQL 那一头的 del_flag=0 过滤，余下逐条走一遍全在内存里
        when(mapper.selectList(any())).thenAnswer(inv -> registry.stream()
                .filter(r -> r.getDelFlag() != null && r.getDelFlag() == 0)
                .collect(Collectors.toList()));
        when(mapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return registry.stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
        });
        service = new TVehRunLineServiceImpl();
        ReflectionTestUtils.setField(service, "vehRunLineMapper", mapper);
    }

    private static Date dt(String s) {
        try {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(s);
        } catch (ParseException e) {
            throw new IllegalStateException(e);
        }
    }

    private TVehRunLine line(long id, String code, String measure, String th1, String th2, String th3,
                             String unit, String start, String end, int prio, int status) {
        TVehRunLine r = new TVehRunLine();
        r.setId(id);
        r.setRuleCode(code);
        r.setRuleName(code);
        r.setMeasureKind(measure);
        r.setTh1Max(th1 == null ? null : new BigDecimal(th1));
        r.setTh2Max(th2 == null ? null : new BigDecimal(th2));
        r.setTh3Max(th3 == null ? null : new BigDecimal(th3));
        r.setUnitKind(unit);
        r.setEffStart(start == null ? null : dt(start));
        r.setEffEnd(end == null ? null : dt(end));
        r.setPriority(prio);
        r.setStatus(status);
        r.setDelFlag(0);
        registry.add(r);
        return r;
    }

    /** 与 doc/schema/veh.sql 里预置的四条界同一份 */
    private void seedFourLines() {
        line(1, "NCX-01", "speed", "80", "100", "120", "km/h", "2019-01-01 00:00:00", null, 10, 0);
        line(2, "NCX-02", "speed", "75", "95", "115", "km/h", "2021-07-01 00:00:00", "2023-07-01 00:00:00", 20, 1);
        line(3, "NCX-03", "speed", "60", "85", "105", "km/h", "2023-07-01 00:00:00", "2026-01-01 00:00:00", 30, 0);
        line(4, "NCX-04", "speed", "70", "90", "110", "km/h", "2026-01-01 00:00:00", null, 40, 0);
    }

    // ---------- 折算与舍法 ----------

    @Test
    void speedReportedInMsIsFoldedToKmhBeforeComparing() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        // 25 m/s × 3.6 = 90 km/h，够着够线不到封顶 → 严管
        VehRunVerdict v = service.judge("speed", new BigDecimal("25"), "m/s", dt("2024-06-01 08:00:00"));
        assertEquals(new BigDecimal("90.000"), v.getConvValue());
        assertEquals(ITVehRunLineService.LEVEL_STRICT, v.getLevelNo());
        assertEquals("严管", v.getConclusion());
        assertEquals("NCX", v.getRuleCode());
    }

    @Test
    void durationReportedInMinutesIsFoldedToHoursBeforeComparing() {
        line(1, "SC", "duration", "4", "6", "8", "h", "2020-01-01 00:00:00", null, 10, 0);
        // 300 分钟 ÷ 60 = 5 小时，够着起算不到够线 → 提示
        VehRunVerdict v = service.judge("duration", new BigDecimal("300"), "min", dt("2024-06-01 08:00:00"));
        assertEquals(new BigDecimal("5.000"), v.getConvValue());
        assertEquals(ITVehRunLineService.LEVEL_HINT, v.getLevelNo());
        assertEquals("提示", v.getConclusion());
    }

    @Test
    void valueAlreadyInRegistryUnitOnlyPassesTheSameRounding() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        VehRunVerdict v = service.judge("speed", new BigDecimal("90"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals(new BigDecimal("90.000"), v.getConvValue());
        assertEquals(ITVehRunLineService.LEVEL_STRICT, v.getLevelNo());
    }

    @Test
    void winterIncidentRoundingIsOneSharedHalfUp() {
        // 去年冬季那条压线的报警：23.611 m/s × 3.6 = 84.9996，
        // 四舍五入到界册精度是 85.000（够线即严管）；直接截尾是 84.999（差一丝，提示）。
        // 取数、比数同一把舍法，两个位置问出来落在同一层。
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        VehRunVerdict v = service.judge("speed", new BigDecimal("23.611"), "m/s", dt("2025-12-15 07:30:00"));
        assertEquals(new BigDecimal("85.000"), v.getConvValue());
        assertEquals(ITVehRunLineService.LEVEL_STRICT, v.getLevelNo());
        // 屏上显的那个数，就是拿去比的那个数
        assertEquals(0, v.getConvValue().compareTo(new BigDecimal("85")));
    }

    @Test
    void unknownUnitPairIsRefusedWithBothUnitsNamed() {
        // 车速的报来口径（m/s）碰上时长的界册单位（h），对不上就是没法折
        line(1, "SC", "duration", "4", "6", "8", "h", "2020-01-01 00:00:00", null, 10, 0);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service.judge("duration", new BigDecimal("10"), "m/s", dt("2024-06-01 08:00:00")));
        assertTrue(e.getMessage().contains("m/s"));
        assertTrue(e.getMessage().contains("h"));
    }

    @Test
    void incompleteMaterialIsRefused() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        Date at = dt("2024-06-01 08:00:00");
        assertThrows(IllegalArgumentException.class, () -> service.judge(null, new BigDecimal("90"), "km/h", at));
        assertThrows(IllegalArgumentException.class, () -> service.judge("speed", null, "km/h", at));
        assertThrows(IllegalArgumentException.class, () -> service.judge("speed", new BigDecimal("90"), null, at));
        assertThrows(IllegalArgumentException.class, () -> service.judge("speed", new BigDecimal("90"), "km/h", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.judge("speed", new BigDecimal("-1"), "km/h", at));
    }

    // ---------- 压线归层（只认一套：归上一层） ----------

    @Test
    void boundaryHitsFallToTheUpperLevel() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        Date at = dt("2024-06-01 08:00:00");
        assertEquals(ITVehRunLineService.LEVEL_HINT,
                service.judge("speed", new BigDecimal("60"), "km/h", at).getLevelNo());
        assertEquals(ITVehRunLineService.LEVEL_STRICT,
                service.judge("speed", new BigDecimal("85"), "km/h", at).getLevelNo());
        assertEquals(ITVehRunLineService.LEVEL_FILE,
                service.judge("speed", new BigDecimal("105"), "km/h", at).getLevelNo());
        assertEquals(ITVehRunLineService.LEVEL_FILE,
                service.judge("speed", new BigDecimal("180"), "km/h", at).getLevelNo());
    }

    @Test
    void belowStartGetsNoLevelButTheSpeakingLineIsStillRecorded() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        VehRunVerdict v = service.judge("speed", new BigDecimal("59.999"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals(ITVehRunLineService.LEVEL_NONE, v.getLevelNo());
        assertEquals("未达起算", v.getConclusion());
        // 界请得出、是数够不上：界次代号照挂
        assertEquals("NCX", v.getRuleCode());
        assertNotNull(v.getEffStart());
    }

    // ---------- 在位窗口 ----------

    @Test
    void inEffectWindowEdges() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h",
                "2024-01-01 00:00:00", "2025-01-01 00:00:00", 10, 0);
        // 起用当日够着即算在位
        assertEquals(1, service.countAvailable(dt("2024-01-01 00:00:00")));
        // 交班当日不含：旧界管到该日之前
        assertEquals(0, service.countAvailable(dt("2025-01-01 00:00:00")));
        assertEquals(1, service.countAvailable(dt("2024-12-31 23:59:59")));
        // 还没到起用那一日
        assertEquals(0, service.countAvailable(dt("2023-12-31 23:59:59")));
    }

    @Test
    void legacyLineWithEmptyHandoverStaysInEffect() {
        line(1, "OLD", "speed", "80", "100", "120", "km/h", "2019-01-01 00:00:00", null, 10, 0);
        assertEquals(1, service.countAvailable(dt("2035-06-01 08:00:00")));
        VehRunVerdict v = service.judge("speed", new BigDecimal("130"), "km/h", dt("2035-06-01 08:00:00"));
        assertEquals("OLD", v.getRuleCode());
        assertEquals(ITVehRunLineService.LEVEL_FILE, v.getLevelNo());
    }

    @Test
    void lineWithoutStartDayNeverStandsIn() {
        line(1, "NOSTART", "speed", "80", "100", "120", "km/h", null, null, 10, 0);
        assertEquals(0, service.countAvailable(dt("2024-06-01 08:00:00")));
    }

    // ---------- 多条在位与交班次序 ----------

    @Test
    void higherHandoverOrderSpeaks() {
        line(1, "LOW", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        line(2, "HIGH", "speed", "70", "90", "110", "km/h", "2020-01-01 00:00:00", null, 20, 0);
        VehRunVerdict v = service.judge("speed", new BigDecimal("95"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals("HIGH", v.getRuleCode());
        assertEquals(ITVehRunLineService.LEVEL_STRICT, v.getLevelNo());
    }

    @Test
    void sameHandoverOrderFallsToLastRuleCode() {
        line(1, "NCX-A", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        line(2, "NCX-B", "speed", "70", "90", "110", "km/h", "2020-01-01 00:00:00", null, 10, 0);
        // 序相同再比界次代号，照排法落在末位的胜出
        VehRunVerdict v = service.judge("speed", new BigDecimal("95"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals("NCX-B", v.getRuleCode());
        // 同一刻再问一遍，答复不许长出两样
        VehRunVerdict again = service.judge("speed", new BigDecimal("95"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals(v.getRuleCode(), again.getRuleCode());
        assertEquals(v.getLevelNo(), again.getLevelNo());
    }

    @Test
    void screenListAndJudgmentComeFromTheSameSource() {
        seedFourLines();
        Date at = dt("2024-09-15 10:00:00");
        List<TVehRunLine> screen = service.listAvailable(at);
        assertFalse(screen.isEmpty());
        VehRunVerdict v = service.judge("speed", new BigDecimal("86"), "km/h", at);
        // 屏上那份在位名单的头一条，就是判定拿去说话的那条
        assertEquals(screen.get(0).getRuleCode(), v.getRuleCode());
        assertEquals(screen.get(0).getEffStart(), v.getEffStart());
    }

    // ---------- 问话的那一刻（补判不许拿新界顶替） ----------

    @Test
    void backfillUsesTheLineInEffectAtThatMoment() {
        seedFourLines();
        // 前年秋天（2024-09）那条 86 km/h 的报警：那一刻 NCX-03 在位，归严管；
        // 今年新定的 NCX-04 若拿去判会低一格（提示）——这种顶替不许再来第二回
        VehRunVerdict v = service.judge("speed", new BigDecimal("86"), "km/h", dt("2024-09-15 10:00:00"));
        assertEquals("NCX-03", v.getRuleCode());
        assertEquals(ITVehRunLineService.LEVEL_STRICT, v.getLevelNo());
        assertEquals(dt("2023-07-01 00:00:00"), v.getEffStart());
    }

    @Test
    void handoverDayReadsOneWayFromBothPositions() {
        seedFourLines();
        Date before = dt("2025-12-31 23:59:59");
        Date turn = dt("2026-01-01 00:00:00");
        // 旧界管到交班日之前
        assertEquals("NCX-03", service.judge("speed", new BigDecimal("95"), "km/h", before).getRuleCode());
        assertEquals("NCX-03", service.listAvailable(before).get(0).getRuleCode());
        // 新界从交班日起头
        assertEquals("NCX-04", service.judge("speed", new BigDecimal("95"), "km/h", turn).getRuleCode());
        assertEquals("NCX-04", service.listAvailable(turn).get(0).getRuleCode());
    }

    // ---------- 已停用与无可用界 ----------

    @Test
    void disabledLineIsListedInRegistryButNeverSpeaks() {
        seedFourLines();
        // NCX-02 已停用：册里翻得着（名单在），只是不许捞它去比数
        List<TVehRunLine> book = service.selectTVehRunLineList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());
        assertTrue(book.stream().anyMatch(r -> "NCX-02".equals(r.getRuleCode())));
        Date at = dt("2022-01-01 08:00:00");
        assertFalse(service.listAvailable(at).stream().anyMatch(r -> "NCX-02".equals(r.getRuleCode())));
        // 它管的那段窗口里，说话的是存量界 NCX-01
        VehRunVerdict v = service.judge("speed", new BigDecimal("95"), "km/h", at);
        assertEquals("NCX-01", v.getRuleCode());
        // 也不许挂在刚得出的结论后头
        assertNotEquals("NCX-02", v.getRuleCode());
    }

    @Test
    void noAvailableLineLeavesRuleFieldsEmpty() {
        VehRunVerdict v = service.judge("speed", new BigDecimal("95"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals(ITVehRunLineService.LEVEL_NONE, v.getLevelNo());
        assertEquals("无可用界", v.getConclusion());
        // 界次代号与起用那一日两栏一并留空，不许抄旁边那条
        assertNull(v.getRuleCode());
        assertNull(v.getEffStart());
        assertNull(v.getConvValue());
    }

    @Test
    void measureKindWithoutLinesIsAlsoNoLine() {
        seedFourLines();
        VehRunVerdict v = service.judge("duration", new BigDecimal("300"), "min", dt("2024-06-01 08:00:00"));
        assertEquals("无可用界", v.getConclusion());
        assertNull(v.getRuleCode());
    }

    // ---------- 界线站不站得住 ----------

    @Test
    void missingBoundaryIsNamedByItsOwnCell() {
        TVehRunLine broken = line(1, "BROKEN", "speed", "60", null, "105", "km/h",
                "2020-01-01 00:00:00", null, 10, 0);
        List<String> problems = service.checkLine(broken.getId());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("够线数值"));
        // 站不住的线不参与比数
        assertEquals(0, service.countAvailable(dt("2024-06-01 08:00:00")));
    }

    @Test
    void notExceedingPreviousBoundaryIsNamed() {
        TVehRunLine flat = line(1, "FLAT", "speed", "60", "85", "85", "km/h",
                "2020-01-01 00:00:00", null, 10, 0);
        List<String> problems = service.checkLine(flat.getId());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("封顶数值"));
        assertTrue(problems.get(0).contains("没越过"));
        assertTrue(problems.get(0).contains("够线数值"));

        TVehRunLine inverted = line(2, "INVERT", "speed", "90", "85", "105", "km/h",
                "2020-01-01 00:00:00", null, 10, 0);
        List<String> problems2 = service.checkLine(inverted.getId());
        assertEquals(1, problems2.size());
        assertTrue(problems2.get(0).contains("够线数值"));
        assertTrue(problems2.get(0).contains("没越过起算数值"));
    }

    @Test
    void unknownLineIsReported() {
        List<String> problems = service.checkLine(999L);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("翻不着"));
    }

    @Test
    void lineWithoutRegistryUnitCannotSpeakAndIsNamed() {
        TVehRunLine noUnit = line(1, "NOUNIT", "speed", "60", "85", "105", null,
                "2020-01-01 00:00:00", null, 10, 0);
        List<String> problems = service.checkLine(noUnit.getId());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("界册单位"));
        // 单位空着的线开不了口，排它后头的正经界接上
        line(2, "BACKUP", "speed", "60", "85", "105", "km/h", "2020-01-01 00:00:00", null, 5, 0);
        VehRunVerdict v = service.judge("speed", new BigDecimal("90"), "km/h", dt("2024-06-01 08:00:00"));
        assertEquals("BACKUP", v.getRuleCode());
    }

    // ---------- 旧办法（签名照原样，口径同一处） ----------

    @Test
    void oldEvaluateByRuleCodeSpeaksTheSameLanguage() {
        seedFourLines();
        Date at = dt("2024-09-15 10:00:00");
        // 旧办法凭界次代号取值（数已是界册单位），落层与 judge 同一处
        assertEquals(ITVehRunLineService.LEVEL_STRICT, service.evaluate("NCX-03", new BigDecimal("86"), at));
        // 压线归上一层，与 judge 同一把尺子
        assertEquals(ITVehRunLineService.LEVEL_STRICT, service.evaluate("NCX-03", new BigDecimal("85"), at));
        // 已停用的不开口
        assertEquals(0, service.evaluate("NCX-02", new BigDecimal("86"), dt("2022-01-01 08:00:00")));
        // 册里没有的代号、缺材料、负数，一律 0
        assertEquals(0, service.evaluate("GONE", new BigDecimal("86"), at));
        assertEquals(0, service.evaluate(null, new BigDecimal("86"), at));
        assertEquals(0, service.evaluate("NCX-03", null, at));
        assertEquals(0, service.evaluate("NCX-03", new BigDecimal("86"), null));
        assertEquals(0, service.evaluate("NCX-03", new BigDecimal("-1"), at));
    }

    @Test
    void evaluateTopTakesTheSpeakingLine() {
        seedFourLines();
        assertEquals(ITVehRunLineService.LEVEL_STRICT,
                service.evaluateTop(new BigDecimal("86"), dt("2024-09-15 10:00:00")));
        assertEquals(0, service.evaluateTop(new BigDecimal("86"), dt("2008-01-01 00:00:00")));
        assertEquals(0, service.evaluateTop(null, dt("2024-09-15 10:00:00")));
    }

    @Test
    void usableAndCountStayConsistentWithTheList() {
        seedFourLines();
        Date at = dt("2024-09-15 10:00:00");
        List<TVehRunLine> avail = service.listAvailable(at);
        assertEquals(avail.size(), service.countAvailable(at));
        for (TVehRunLine r : avail) {
            assertTrue(service.usable(r.getId(), at));
        }
        // 已停用的那条：usable 与名单一个口径
        assertFalse(service.usable(2L, at));
        assertFalse(service.usable(null, at));
        assertFalse(service.usable(1L, null));
    }
}
