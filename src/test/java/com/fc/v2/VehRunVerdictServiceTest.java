package com.fc.v2;

import com.fc.v2.mapper.auto.TVehRunLineMapper;
import com.fc.v2.mapper.auto.TVehRunVerdictMapper;
import com.fc.v2.model.auto.TVehRunLine;
import com.fc.v2.model.auto.TVehRunVerdict;
import com.fc.v2.service.ITVehRunLineService;
import com.fc.v2.service.impl.TVehRunLineServiceImpl;
import com.fc.v2.service.impl.TVehRunVerdictServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

/**
 * 结论落库与界次对账：栏位照判定那一处说的话写；
 * 屏上仍挂着界次代号、册里那一刻却请不出在位界的单子，逐条指出。
 */
class VehRunVerdictServiceTest {

    private List<TVehRunLine> registry;
    private TVehRunVerdictMapper verdictMapper;
    private TVehRunVerdictServiceImpl verdictService;

    @BeforeEach
    void setUp() {
        registry = new ArrayList<>();
        TVehRunLineMapper lineMapper = mock(TVehRunLineMapper.class);
        when(lineMapper.selectList(any())).thenAnswer(inv -> registry.stream()
                .filter(r -> r.getDelFlag() != null && r.getDelFlag() == 0)
                .collect(Collectors.toList()));
        when(lineMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return registry.stream().filter(r -> id.equals(r.getId())).findFirst().orElse(null);
        });
        TVehRunLineServiceImpl lineService = new TVehRunLineServiceImpl();
        ReflectionTestUtils.setField(lineService, "vehRunLineMapper", lineMapper);

        verdictMapper = mock(TVehRunVerdictMapper.class);
        when(verdictMapper.insert(any())).thenReturn(1);

        verdictService = new TVehRunVerdictServiceImpl();
        ReflectionTestUtils.setField(verdictService, "vehRunVerdictMapper", verdictMapper);
        ReflectionTestUtils.setField(verdictService, "vehRunLineService", lineService);
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
        r.setMeasureKind(measure);
        r.setTh1Max(new BigDecimal(th1));
        r.setTh2Max(new BigDecimal(th2));
        r.setTh3Max(new BigDecimal(th3));
        r.setUnitKind(unit);
        r.setEffStart(dt(start));
        r.setEffEnd(end == null ? null : dt(end));
        r.setPriority(prio);
        r.setStatus(status);
        r.setDelFlag(0);
        registry.add(r);
        return r;
    }

    @Test
    void storedFieldsAreWrittenAsTheDecisionSays() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2023-07-01 00:00:00", null, 30, 0);
        Date at = dt("2024-09-15 10:00:00");
        TVehRunVerdict row = verdictService.judgeAndStore("BJ-2024-0001", "speed",
                new BigDecimal("23.611"), "m/s", at);

        ArgumentCaptor<TVehRunVerdict> captor = ArgumentCaptor.forClass(TVehRunVerdict.class);
        verify(verdictMapper).insert(captor.capture());
        TVehRunVerdict stored = captor.getValue();
        assertSame(row, stored);
        assertEquals("BJ-2024-0001", stored.getAlarmNo());
        // 折算、层、结论栏、界次代号、起用那一日，全照判定那一处说的话写
        assertEquals(new BigDecimal("85.000"), stored.getConvValue());
        assertEquals(ITVehRunLineService.LEVEL_STRICT, stored.getLevelNo());
        assertEquals("严管", stored.getConclusion());
        assertEquals("NCX", stored.getRuleCode());
        assertEquals(dt("2023-07-01 00:00:00"), stored.getEffStart());
        assertEquals(at, stored.getJudgedAt());
        assertEquals(0, stored.getDelFlag());
    }

    @Test
    void noLineVerdictLeavesRuleFieldsEmpty() {
        TVehRunVerdict row = verdictService.judgeAndStore("BJ-2024-0002", "speed",
                new BigDecimal("90"), "km/h", dt("2024-09-15 10:00:00"));
        assertEquals("无可用界", row.getConclusion());
        assertNull(row.getRuleCode());
        assertNull(row.getEffStart());
    }

    @Test
    void incompleteMaterialStoresNothing() {
        line(1, "NCX", "speed", "60", "85", "105", "km/h", "2023-07-01 00:00:00", null, 30, 0);
        Date at = dt("2024-09-15 10:00:00");
        // 报警代号空着，结论没处挂
        assertThrows(IllegalArgumentException.class,
                () -> verdictService.judgeAndStore(" ", "speed", new BigDecimal("90"), "km/h", at));
        // 问话材料缺任一个，判定那一处直接拒绝，不落半成品
        assertThrows(IllegalArgumentException.class,
                () -> verdictService.judgeAndStore("BJ-1", null, new BigDecimal("90"), "km/h", at));
        assertThrows(IllegalArgumentException.class,
                () -> verdictService.judgeAndStore("BJ-1", "speed", new BigDecimal("90"), "km/h", null));
        verify(verdictMapper, never()).insert(any());
    }

    @Test
    void danglingPointsOutRowsWhoseLineNoLongerSpeaks() {
        TVehRunLine ncx = line(1, "NCX", "speed", "60", "85", "105", "km/h",
                "2023-07-01 00:00:00", null, 30, 0);
        Date at = dt("2024-09-15 10:00:00");

        TVehRunVerdict kept = verdict("BJ-KEEP", "NCX", dt("2023-07-01 00:00:00"), at);
        TVehRunVerdict noLine = verdict("BJ-NOLINE", null, null, at);
        List<TVehRunVerdict> stored = new ArrayList<>();
        stored.add(kept);
        stored.add(noLine);
        when(verdictMapper.selectList(any())).thenReturn(stored);

        // 界还在位、代号没挪：账面对得上
        assertTrue(verdictService.listDangling().isEmpty());

        // 界被停用之后：屏上仍挂着界次代号、册里那一刻请不出在位界的单子被指出
        ncx.setStatus(1);
        List<TVehRunVerdict> diff = verdictService.listDangling();
        assertEquals(1, diff.size());
        assertEquals("BJ-KEEP", diff.get(0).getAlarmNo());
        // 无可用界的单子屏上没挂界次代号，不在这条对账线上
        assertFalse(diff.stream().anyMatch(r -> "BJ-NOLINE".equals(r.getAlarmNo())));
    }

    @Test
    void danglingAlsoPointsOutRowsWhoseLineWasSwapped() {
        line(1, "NCX-OLD", "speed", "60", "85", "105", "km/h", "2023-07-01 00:00:00", null, 30, 0);
        Date at = dt("2024-09-15 10:00:00");
        TVehRunVerdict row = verdict("BJ-SWAP", "NCX-OLD", dt("2023-07-01 00:00:00"), at);
        when(verdictMapper.selectList(any())).thenReturn(new ArrayList<>(java.util.Collections.singletonList(row)));

        // 后来同刻又落了一条交班次序更高的界：册里那一刻请出的已不是单子上挂的那条
        line(2, "NCX-NEW", "speed", "70", "90", "110", "km/h", "2023-07-01 00:00:00", null, 40, 0);
        List<TVehRunVerdict> diff = verdictService.listDangling();
        assertEquals(1, diff.size());
        assertEquals("BJ-SWAP", diff.get(0).getAlarmNo());
    }

    private TVehRunVerdict verdict(String alarmNo, String ruleCode, Date effStart, Date judgedAt) {
        TVehRunVerdict v = new TVehRunVerdict();
        v.setAlarmNo(alarmNo);
        v.setMeasureKind("speed");
        v.setReportValue(new BigDecimal("90"));
        v.setReportUnit("km/h");
        v.setJudgedAt(judgedAt);
        v.setLevelNo(ruleCode == null ? 0 : 2);
        v.setConclusion(ruleCode == null ? "无可用界" : "严管");
        v.setRuleCode(ruleCode);
        v.setEffStart(effStart);
        v.setDelFlag(0);
        return v;
    }
}
