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
import top.enderliquid.audioflow.common.enums.LimitType;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.song.SongCompleteUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongPageDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongSaveDTO;
import top.enderliquid.audioflow.dto.request.song.SongUpdateDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
import top.enderliquid.audioflow.dto.response.SongVO;
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
     * 上传歌曲
     * 需要登录
     * 已废弃，请使用prepareUpload和completeUpload
     */
    @SaCheckLogin
    @PostMapping
    @RateLimit(
            refillRate = "3/60",
            capacity = 3,
            limitType = LimitType.BOTH,
            message = "上传过于频繁，请稍后再试"
    )
    @Deprecated
    public HttpResponseBody<SongVO> uploadSong(@Valid @ModelAttribute SongSaveDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.saveSong(dto, userId);
        return HttpResponseBody.ok(songVO, "上传成功");
    }

    /**
     * 准备上传歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("/prepare")
    @RateLimit(
            refillRate = "3/60",
            capacity = 3,
            limitType = LimitType.BOTH,
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
    public HttpResponseBody<SongVO> completeUpload(@Valid @RequestBody SongCompleteUploadDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.completeUpload(dto, userId);
        return HttpResponseBody.ok(songVO);
    }

    /**
     * 分页查询/搜索歌曲
     */
    @GetMapping
    @RateLimit(
            refillRate = "1/1",
            capacity = 5,
            limitType = LimitType.IP,
            message = "查询过于频繁，请稍后再试"
    )
    public HttpResponseBody<CommonPageVO<SongVO>> pageSongs(@Valid @ModelAttribute SongPageDTO dto) {
        CommonPageVO<SongVO> result = songService.pageSongsByUploaderKeywordAndSongKeyword(dto);
        return HttpResponseBody.ok(result);
    }

    /**
     * 删除自己的歌曲
     * 需要登录
     */
    @SaCheckLogin
    @DeleteMapping("{songId}")
    @RateLimit(
            refillRate = "1/1",
            capacity = 5,
            limitType = LimitType.BOTH,
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
    @RateLimit(limitType = LimitType.IP)
    public HttpResponseBody<SongVO> getSongInfo(@PathVariable Long songId) {
        SongVO songVO = songService.getSong(songId);
        return HttpResponseBody.ok(songVO, null);
    }

    /**
     * 获取歌曲播放URL
     */
    @GetMapping("{songId}/play")
    @RateLimit(
            refillRate = "1/1",
            capacity = 5,
            limitType = LimitType.IP,
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
    @RateLimit
    public HttpResponseBody<SongVO> updateSong(@PathVariable Long songId, @Valid @RequestBody SongUpdateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.updateSong(dto, songId, userId);
        return HttpResponseBody.ok(songVO);
    }


}
