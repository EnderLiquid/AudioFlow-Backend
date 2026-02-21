package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.SongPageDTO;
import top.enderliquid.audioflow.dto.request.SongSaveDTO;
import top.enderliquid.audioflow.dto.request.SongUpdateDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
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
     */
    @SaCheckLogin
    @PostMapping
    public HttpResponseBody<SongVO> uploadSong(@Valid @ModelAttribute SongSaveDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.saveSong(dto, userId);
        return HttpResponseBody.ok(songVO, "上传成功");
    }

    /**
     * 分页查询/搜索歌曲
     */
    @GetMapping
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
    public HttpResponseBody<Void> removeSong(@PathVariable Long songId) {
        long userId = StpUtil.getLoginIdAsLong();
        songService.removeSong(songId, userId);
        return HttpResponseBody.ok(null, "删除成功");
    }

    /**
     * 管理员强制删除歌曲
     * 需要 ADMIN 角色
     */
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @DeleteMapping("{songId}/force")
    public HttpResponseBody<Void> removeSongForce(@PathVariable Long songId) {
        songService.removeSongForce(songId);
        return HttpResponseBody.ok(null, "管理员强制删除成功");
    }

    /**
     * 获取歌曲信息
     */
    @GetMapping("{songId}")
    public HttpResponseBody<SongVO> getSongInfo(@PathVariable Long songId) {
        SongVO songVO = songService.getSong(songId);
        return HttpResponseBody.ok(songVO, null);
    }

    /**
     * 获取歌曲播放URL
     */
    @GetMapping("{songId}/play")
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
    public HttpResponseBody<SongVO> updateSong(@PathVariable Long songId, @Valid @RequestBody SongUpdateDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        SongVO songVO = songService.updateSong(dto, songId, userId);
        return HttpResponseBody.ok(songVO);
    }

    /**
     * 管理员强制更新歌曲信息
     * 需要 ADMIN 角色
     */
    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PatchMapping("{songId}/force")
    public HttpResponseBody<SongVO> updateSongForce(@PathVariable Long songId, @Valid @RequestBody SongUpdateDTO dto) {
        SongVO songVO = songService.updateSongForce(dto, songId);
        return HttpResponseBody.ok(songVO);
    }
}
