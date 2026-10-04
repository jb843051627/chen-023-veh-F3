package com.fc.v2.controller.admin;

import java.util.Date;
import java.util.List;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.base.BaseController;
import com.fc.v2.common.domain.AjaxResult;
import com.fc.v2.common.domain.ResultTable;
import com.fc.v2.common.log.Log;
import com.fc.v2.model.auto.TVehRunLine;
import com.fc.v2.service.ITVehRunLineService;
import com.fc.v2.util.StringUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 运行界次判定线 Controller（界册翻检与在位名单；归层问话走 /vehRunVerdict/judge 那一道门）
 *
 * @author fuce
 * @date 2026-10-04
 */
@Api(value = "运行界次判定线")
@Controller
@RequestMapping("/vehRunLine")
public class VehRunLineController extends BaseController {

    @Autowired
    private ITVehRunLineService vehRunLineService;

    @Log(title = "界册名单", action = "list")
    @ApiOperation(value = "界册名单", notes = "册里翻得着、屏上列得出：含已停用，剔删除标记")
    @GetMapping("/list")
    @RequiresPermissions("veh:vehRunLine:list")
    @ResponseBody
    public ResultTable list(TVehRunLine record) {
        QueryWrapper<TVehRunLine> queryWrapper = new QueryWrapper<TVehRunLine>();
        queryWrapper.eq("del_flag", 0);
        if (StringUtils.isNotEmpty(record.getRuleCode())) {
            queryWrapper.eq("rule_code", record.getRuleCode());
        }
        if (StringUtils.isNotEmpty(record.getMeasureKind())) {
            queryWrapper.eq("measure_kind", record.getMeasureKind());
        }
        if (record.getStatus() != null) {
            queryWrapper.eq("status", record.getStatus());
        }
        queryWrapper.orderByDesc("priority").orderByDesc("rule_code");
        startPage();
        com.github.pagehelper.PageInfo<TVehRunLine> page =
                new com.github.pagehelper.PageInfo<TVehRunLine>(vehRunLineService.selectTVehRunLineList(queryWrapper));
        return pageTable(page.getList(), page.getTotal());
    }

    @Log(title = "在位名单", action = "effective")
    @ApiOperation(value = "在位名单", notes = "at 不传按眼下；与归层判定逐条走一遍凑出来的名单同出一处")
    @GetMapping("/effective")
    @RequiresPermissions("veh:vehRunLine:list")
    @ResponseBody
    public AjaxResult effective(@RequestParam(required = false) Date at) {
        Date moment = at != null ? at : new Date();
        return success(200, "在位名单与归层判定同出一处", vehRunLineService.listAvailable(moment));
    }

    @Log(title = "界线核验", action = "check")
    @ApiOperation(value = "界线核验", notes = "这条界线站不站得住，回话指名哪一格空着、哪一格没越过前头那一格")
    @GetMapping("/check")
    @RequiresPermissions("veh:vehRunLine:check")
    @ResponseBody
    public AjaxResult check(Long id) {
        List<String> problems = vehRunLineService.checkLine(id);
        if (problems.isEmpty()) {
            return success("这条界线站得住");
        }
        return success(200, "这条界线站不住", problems);
    }
}
