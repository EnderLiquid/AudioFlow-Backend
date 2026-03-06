package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.annotation.RateLimit;
import top.enderliquid.audioflow.common.annotation.RateLimits;
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.song.*;
import top.enderliquid.audioflow.dto.response.BatchResult;
import top.enderliquid.audioflow.dto.response.PageVO;
import top.enderliquid.audioflow.dto.response.song.SongUploadPrepareVO;
import top.enderliquid.audioflow.dto.response.song.SongVO;
import top.enderliquid.audioflow.service.SongService;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/songs")
@Validated
public class SongController {

    @Autowired
    private SongService songService;

    /**
     * 准备上传歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("/prepare")
    @RateLimits(
            value = {
                    @RateLimit(type = LimitType.IP, refillRate = "1/1", capacity = 5),
                    @RateLimit(type = LimitType.USER, refillRate = "1/1", capacity = 5)
            },
            message = "上传过于频繁，请稍后再试"
    )
    public HttpResponseBody<SongUploadPrepareVO> prepareUpload(@Valid @RequestBody SongPrepareUploadDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongUploadPrepareVO prepareVO = songService.prepareUpload(dto, userId);
        return HttpResponseBody.ok(prepareVO);
    }

    /**
     * 完成上传歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("/complete")
    @RateLimits(
            value = {
                    @RateLimit(type = LimitType.IP),
                    @RateLimit(type = LimitType.USER)
            }
    )
    public HttpResponseBody<SongVO> completeUpload(@Valid @RequestBody SongCompleteUploadDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.completeUpload(dto, userId);
        return HttpResponseBody.ok(songVO);
    }

    /**
     * 分页查询/搜索歌曲
     */
    @GetMapping
    @RateLimits(
            value = @RateLimit(type = LimitType.IP, refillRate = "1/1", capacity = 5),
            message = "查询过于频繁，请稍后再试"
    )
    public HttpResponseBody<PageVO<SongVO>> pageSongs(@Valid @ModelAttribute SongPageDTO dto) {
        PageVO<SongVO> result = songService.pageSongsByUploaderKeywordAndSongKeyword(dto);
        return HttpResponseBody.ok(result);
    }

    /**
     * 删除自己的歌曲
     * 需要登录
     */
    @SaCheckLogin
    @DeleteMapping("{songId}")
    @RateLimits(
            value = {
                    @RateLimit(type = LimitType.IP, refillRate = "1/1", capacity = 5),
                    @RateLimit(type = LimitType.USER, refillRate = "1/1", capacity = 5)
            },
            message = "删除过于频繁，请稍后再试"
    )
    public HttpResponseBody<Void> removeSong(@PathVariable Long songId) {
        long userId = StpUtil.getLoginIdAsLong();
        songService.removeSong(songId, userId);
        return HttpResponseBody.ok(null, "删除成功");
    }

    /**
     * 获取歌曲信息
     */
    @GetMapping("{songId}")
    @RateLimits(@RateLimit(type = LimitType.IP))
    public HttpResponseBody<SongVO> getSongInfo(@PathVariable Long songId) {
        SongVO songVO = songService.getSong(songId);
        return HttpResponseBody.ok(songVO, null);
    }

    /**
     * 获取歌曲播放URL
     */
    @GetMapping("{songId}/play")
    @RateLimits(
            value = @RateLimit(type = LimitType.IP, refillRate = "1/1", capacity = 5),
            message = "获取Url过于频繁，请稍后再试"
    )
    public void getSongUrl(@PathVariable Long songId, HttpServletResponse response) {
        String url = songService.getSongUrl(songId);
        try {
            if (url == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "获取歌曲文件URL失败");
                return;
            }
            response.sendRedirect(url);
        } catch (IOException e) {
            log.error("歌曲URL查询接口响应失败", e);
        }
}

    /**
     * 更新自己的歌曲信息
     * 需要登录
     */
    @SaCheckLogin
    @PatchMapping("{songId}")
    @RateLimits({
            @RateLimit(type = LimitType.IP),
            @RateLimit(type = LimitType.USER)
    })
    public HttpResponseBody<SongVO> updateSong(@PathVariable Long songId, @Valid @RequestBody SongUpdateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.updateSong(dto, songId, userId);
        return HttpResponseBody.ok(songVO);
    }

    /**
     * 批量准备上传歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("/batch-prepare")
    @RateLimits(
            value = {
                    @RateLimit(type = LimitType.IP, refillRate = "1/10", capacity = 3),
                    @RateLimit(type = LimitType.USER, refillRate = "1/10", capacity = 3)
            },
            message = "批量准备上传过于频繁，请稍后再试"
    )
    public HttpResponseBody<BatchResult<SongUploadPrepareVO>> batchPrepareUpload(@Valid @RequestBody SongBatchPrepareDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BatchResult<SongUploadPrepareVO> result = songService.batchPrepareUpload(dto, userId);
        return HttpResponseBody.ok(result);
    }

    /**
     * 批量完成上传歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("/batch-complete")
    @RateLimits(
            value = {
                    @RateLimit(type = LimitType.IP, refillRate = "1/10", capacity = 3),
                    @RateLimit(type = LimitType.USER, refillRate = "1/10", capacity = 3)
            },
            message = "批量完成上传过于频繁，请稍后再试"
    )
    public HttpResponseBody<BatchResult<SongVO>> batchCompleteUpload(@Valid @RequestBody SongBatchCompleteDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BatchResult<SongVO> result = songService.batchCompleteUpload(dto, userId);
        return HttpResponseBody.ok(result);
    }

    /**
     * 批量删除歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("/batch")
    @RateLimits(
            value = {
                    @RateLimit(type = LimitType.IP, refillRate = "1/10", capacity = 3),
                    @RateLimit(type = LimitType.USER, refillRate = "1/10", capacity = 3)
            },
            message = "批量删除过于频繁，请稍后再试"
    )
    public HttpResponseBody<BatchResult<Object>> batchRemoveSongs(@Valid @RequestBody SongBatchDeleteDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BatchResult<Object> result = songService.batchRemoveSongs(dto, userId);
        return HttpResponseBody.ok(result);
    }
}