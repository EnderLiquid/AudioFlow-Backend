package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.response.checkin.CheckinResultVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinStatusVO;
import top.enderliquid.audioflow.dto.response.checkin.CheckinSummaryVO;
import top.enderliquid.audioflow.service.CheckinService;

@RestController
@RequestMapping("/api/checkins")
@Validated
@RequiredArgsConstructor
public class CheckinController {

    private final CheckinService checkinService;

    /**
     * 用户签到
     */
    @SaCheckLogin
    @PostMapping
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<CheckinResultVO> checkin() {
        long userId = StpUtil.getLoginIdAsLong();
        CheckinResultVO result = checkinService.checkin(userId);
        return HttpResponseBody.ok(result, "签到成功");
    }

    /**
     * 查询今日签到状态
     */
    @SaCheckLogin
    @GetMapping("/today")
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<CheckinStatusVO> getTodayStatus() {
        long userId = StpUtil.getLoginIdAsLong();
        CheckinStatusVO status = checkinService.getTodayStatus(userId);
        return HttpResponseBody.ok(status, "查询签到状态成功");
    }

    /**
     * 查询签到统计
     */
    @SaCheckLogin
    @GetMapping("/summary")
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<CheckinSummaryVO> getSummary() {
        long userId = StpUtil.getLoginIdAsLong();
        CheckinSummaryVO summary = checkinService.getSummary(userId);
        return HttpResponseBody.ok(summary, "查询签到统计成功");
    }
}