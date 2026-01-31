package top.enderliquid.audioflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.enderliquid.audioflow.common.response.HttpResponseBody;
import top.enderliquid.audioflow.dto.request.SongPageDTO;
import top.enderliquid.audioflow.dto.request.SongSaveDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongVO;
import top.enderliquid.audioflow.service.SongService;

@RestController
@RequestMapping("/api/song/")
@Validated
public class SongController {

    @Autowired
    private SongService songService;

    /**
     * 上传歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("upload")
    public HttpResponseBody<SongVO> uploadSong(@ModelAttribute SongSaveDTO dto) {
        // 获取当前登录用户 ID
        long userId = StpUtil.getLoginIdAsLong();
        // 调用 Service
        SongVO songVO = songService.saveSong(dto, userId);
        return HttpResponseBody.ok(songVO, "上传成功");
    }

    /**
     * 分页查询/搜索歌曲
     */
    @GetMapping("page")
    public HttpResponseBody<CommonPageVO<SongVO>> pageSongs(@ModelAttribute SongPageDTO dto) {
        CommonPageVO<SongVO> result = songService.pageSongsByUploaderKeywordAndSongKeyword(dto);
        return HttpResponseBody.ok(result);
    }

    /**
     * 删除自己的歌曲
     * 需要登录
     */
    @SaCheckLogin
    @PostMapping("remove")
    public HttpResponseBody<Void> removeSong(@RequestParam("id") Long songId) {
        long userId = StpUtil.getLoginIdAsLong();
        songService.removeSong(songId, userId);
        return HttpResponseBody.ok(null, "删除成功");
    }

    /**
     * 管理员强制删除歌曲
     * 需要 ADMIN 角色
     */
    @SaCheckRole("ADMIN")
    @PostMapping("remove/admin")
    public HttpResponseBody<Void> removeSongForce(@RequestParam("id") Long songId) {
        songService.removeSongForce(songId);
        return HttpResponseBody.ok(null, "管理员强制删除成功");
    }
}
