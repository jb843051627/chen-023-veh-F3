package com.fc.v2.controller.admin;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehRunVerdict;
import com.fc.v2.model.custom.VehRunVerdictAsk;
import com.fc.v2.service.ITVehRunVerdictService;
import com.fc.v2.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 报警归层结论 Controller（归层只有这一道门：递问话材料，层与界次栏位照判定那一处说的话写）
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "报警归层结论")
@Controller
@RequestMapping("/vehRunVerdict")
public class VehRunVerdictController extends BaseController {

    @Autowired
    private ITVehRunVerdictService vehRunVerdictService;

    @Log(title = "归层判定", action = "judge")
    @ApiOperation(value = "归层判定", notes = "只收问话材料；层、结论栏、界次代号、起用那一日由判定那一处作答后一并入库")
    @PostMapping("/judge")
    @RequiresPermissions("veh:vehRunVerdict:judge")
    @ResponseBody
    public AjaxResult judge(@RequestBody VehRunVerdictAsk ask) {
        try {
            TVehRunVerdict row = vehRunVerdictService.judgeAndStore(ask.getAlarmNo(), ask.getMeasureKind(),
                    ask.getReportValue(), ask.getReportUnit(), ask.getJudgedAt());
            return success(200, "归层办结", row);
        } catch (IllegalArgumentException e) {
            return error(e.getMessage());
        }
    }

    @Log(title = "结论名单", action = "list")
    @ApiOperation(value = "结论名单", notes = "屏上那份结论名单，按问话那一刻倒序")
    @GetMapping("/list")
    @RequiresPermissions("veh:vehRunVerdict:list")
    @ResponseBody
    public ResultTable list(TVehRunVerdict record) {
        QueryWrapper<TVehRunVerdict> queryWrapper = new QueryWrapper<TVehRunVerdict>();
        queryWrapper.eq("del_flag", 0);
        if (StringUtils.isNotEmpty(record.getAlarmNo())) {
            queryWrapper.eq("alarm_no", record.getAlarmNo());
        }
        if (StringUtils.isNotEmpty(record.getRuleCode())) {
            queryWrapper.eq("rule_code", record.getRuleCode());
        }
        if (record.getLevelNo() != null) {
            queryWrapper.eq("level_no", record.getLevelNo());
        }
        queryWrapper.orderByDesc("judged_at").orderByDesc("id");
        startPage();
        com.github.pagehelper.PageInfo<TVehRunVerdict> page =
                new com.github.pagehelper.PageInfo<TVehRunVerdict>(vehRunVerdictService.selectTVehRunVerdictList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "界次对账", action = "dangling")
    @ApiOperation(value = "界次对账", notes = "屏上仍挂着界次代号、册里那一刻却请不出在位界的单子，逐条指出")
    @GetMapping("/dangling")
    @RequiresPermissions("veh:vehRunVerdict:dangling")
    @ResponseBody
    public AjaxResult dangling() {
        List<TVehRunVerdict> diff = vehRunVerdictService.listDangling();
        if (diff.isEmpty()) {
            return success("屏上挂的界次代号与界册对得上，没有差开的单子");
        }
        return success(200, "差开" + diff.size() + "条，逐条指出", diff);
    }
}
