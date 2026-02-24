package top.enderliquid.audioflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.helpers.DefaultHandler;
import top.enderliquid.audioflow.common.enums.Role;
import top.enderliquid.audioflow.common.enums.SongStatus;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.dto.param.SongPageParam;
import top.enderliquid.audioflow.dto.request.song.SongCompleteUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongPageDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.request.song.SongSaveDTO;
import top.enderliquid.audioflow.dto.request.song.SongUpdateDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
import top.enderliquid.audioflow.dto.response.SongVO;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.SongService;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class SongServiceImpl implements SongService {

@Autowired
    private UserManager userManager;

    @Autowired
    private SongManager songManager;

    @Autowired
    private OSSManager ossManager;

    private static final Tika TIKA = new Tika();
    private static final Parser TIKA_PARSER = new AutoDetectParser();

    private static final Map<String, String> MIME_TYPE_TO_EXTENSION_MAP = new HashMap<>();

    static {
        // MP3 类型
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/mpeg", "mp3");
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/mp3", "mp3");

        // WAV 类型
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/wav", "wav");
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/x-wav", "wav");
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/vnd.wave", "wav");

        // OGG 类型
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/ogg", "ogg");
        MIME_TYPE_TO_EXTENSION_MAP.put("application/ogg", "ogg");

        // FLAC 类型
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/flac", "flac");
        MIME_TYPE_TO_EXTENSION_MAP.put("audio/x-flac", "flac");
    }

    @Override
    public SongVO saveSong(SongSaveDTO dto, Long userId) {
        log.info("请求上传歌曲，用户ID: {}", userId);
        // 检测文件是否为空
        if (dto.getFile().isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        // 检查用户是否存在
        User uploader = userManager.getById(userId);
        if (uploader == null) {
            throw new BusinessException("用户不存在");
        }
        String name = dto.getName();
        // 提前生成歌曲Id
        long songId = IdWorker.getId();
        // 获取原始文件名
        if (name == null) {
            String defaultName = String.valueOf(songId);
            // Opera浏览器的MultipartFile对象
            // 调用getOriginalFilename()可能返回带路径的值，
            // 因此需要显式分离出文件全称。
            // 注意，MultipartFile对象调用getOriginalFilename()可能返回null。
            String originFileName = StringUtils.getFilename(dto.getFile().getOriginalFilename());
            name = getNameFromOriginFileName(originFileName);
            if (name == null) name = defaultName;
        }
        // 忽略原始文件扩展名，通过Magic Number推断文件类型
        String mimeType;
        InputStream inputStream = getInputStream(dto.getFile());
        if (inputStream == null) throw new BusinessException("读取文件失败");
        try {
            mimeType = TIKA.detect(inputStream);
        } catch (IOException e) {
            throw new BusinessException("无法获取文件类型", e);
        }
        if (mimeType == null) {
            throw new BusinessException("无法获取文件类型");
        }
        String extension = MIME_TYPE_TO_EXTENSION_MAP.get(mimeType);
        if (extension == null) {
            throw new BusinessException("不支持该文件类型");
        }
        String fileName = songId + "." + extension;
log.info("歌曲文件通过检验，歌曲名称: {}", name);
        // 保存歌曲文件
        inputStream = getInputStream(dto.getFile());
        if (inputStream == null) throw new BusinessException("读取文件失败");
        if (!ossManager.deleteFile(fileName)) {
            log.warn("删除已存在的文件失败: {}", fileName);
        }
        // 将保存操作注释掉，因为现在的流程是前端直接上传到OSS
        // 如果需要支持旧的saveSong接口，需要使用不同的上传方式
        log.warn("saveSong方法已废弃，请使用prepareUpload和completeUpload新流程");
        throw new BusinessException("上传方式已变更，请使用新的上传接口");
    }

    @Nullable
    private InputStream getInputStream(MultipartFile file) {
        try {
            return file.getInputStream();
        } catch (IOException e) {
            log.error("获取文件输入流失败：{}", file.getName());
            return null;
        }
    }

    // 从文件全称获取文件名
    @Nullable
    private String getNameFromOriginFileName(String originFileName) {
        if (originFileName == null) {
            return null;
        }
        originFileName = originFileName.trim();
        if (originFileName.isEmpty()) {
            return null;
        }
        String name;
        // 获取第一个'.'的下标
        int index = originFileName.lastIndexOf('.');
        if (index == -1) {
            // 文件全称中没有'.'
            name = originFileName;
        } else {
            name = originFileName.substring(0, index).trim();
        }
        return name.substring(0, Math.min(name.length(), 63));
    }

    @Nullable
    private Long getAudioDurationInMills(InputStream inputStream) {
        DefaultHandler handler = new DefaultHandler();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        String durationStr;
        try (inputStream) {
            TIKA_PARSER.parse(inputStream, handler, metadata, parseContext);
            durationStr = metadata.get(XMPDM.DURATION);
        } catch (Exception e) {
            return null;
        }
        if (durationStr == null || durationStr.isEmpty()) {
            return null;
        }
        double durationSeconds;
        try {
            durationSeconds = Double.parseDouble(durationStr);
        } catch (NumberFormatException e) {
            return null;
        }
        return Math.round(durationSeconds * 1000);
    }

    @Override
    public CommonPageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(SongPageDTO dto) {
        log.info("请求分页查询歌曲");
        SongPageParam param = new SongPageParam();
        BeanUtils.copyProperties(dto, param);
        if (param.getIsAsc() == null) param.setIsAsc(false);
        if (param.getPageIndex() == null) param.setPageIndex(1L);
        if (param.getPageSize() == null) param.setPageSize(10L);
        IPage<SongBO> songBOPage = songManager.pageByUploaderKeywordAndSongKeyword(param);
        List<SongBO> songBOList = songBOPage.getRecords();
        List<SongVO> songVOList = new ArrayList<>();
        if (songBOList != null && !songBOList.isEmpty()) {
            for (SongBO songBO : songBOList) {
                if (songBO == null) continue;
                SongVO songVO = new SongVO();
                BeanUtils.copyProperties(songBO, songVO);
                songVOList.add(songVO);
            }
        }
        CommonPageVO<SongVO> pageVO = new CommonPageVO<>();
        pageVO.setList(songVOList);
        pageVO.setPageIndex(songBOPage.getCurrent());
        pageVO.setPageSize(songBOPage.getSize());
        pageVO.setTotal(songBOPage.getTotal());
        log.info("分页查询歌曲成功");
        return pageVO;
    }

    @Override
    public void removeSong(Long songId, Long userId) {
        log.info("请求删除歌曲，用户ID: {} ，歌曲ID: {}", userId, songId);
        Song song = songManager.getById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        // 查询当前用户信息判断权限
        User user = userManager.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 判断权限：管理员可以删除任何歌曲，普通用户只能删除自己的歌曲
        if (!(user.getRole() == Role.ADMIN) && !song.getUploaderId().equals(userId)) {
            throw new BusinessException("无权删除他人上传的歌曲");
        }
        if (!songManager.removeById(song)) {
            log.error("删除歌曲信息失败");
            throw new BusinessException("删除歌曲信息失败");
        }
log.info("删除歌曲信息成功");
        String fileName = song.getFileName();
        if (!ossManager.deleteFile(fileName)) {
            log.error("删除歌曲文件失败");
        } else {
            log.info("删除歌曲文件成功");
        }
    }

    @Override
    public SongVO getSong(Long songId) {
        log.info("请求获取歌曲信息，歌曲ID: {}", songId);
        Song song = songManager.getById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        SongVO songVO = new SongVO();
        BeanUtils.copyProperties(song, songVO);
        User uploader = userManager.getById(song.getUploaderId());
        if (uploader != null) songVO.setUploaderName(uploader.getName());
        log.info("获取歌曲信息成功");
        return songVO;
    }

@Override
    public String getSongUrl(Long songId) {
        log.info("请求获取歌曲播放链接，歌曲ID: {}", songId);
        Song song = songManager.getById(songId);
        if (song == null) {
            return null;
        }
        String url = ossManager.getPresignedGetUrl(song.getFileName(), Duration.ofSeconds(3600));
        if (url != null) log.info("获取歌曲播放链接成功");
        return url;
    }

    @Override
    public SongVO updateSong(SongUpdateDTO dto, Long songId, Long userId) {
        log.info("请求更新歌曲信息，歌曲ID: {}", songId);
        if (dto.getName() == null && dto.getDescription() == null) {
            throw new BusinessException("更新信息不能全为空");
        }
        Song song = songManager.getById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        // 查询当前用户信息判断权限
        User user = userManager.getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 判断权限：管理员可以更新任何歌曲，普通用户只能更新自己的歌曲
        if (!(user.getRole() == Role.ADMIN) && !song.getUploaderId().equals(userId)) {
            throw new BusinessException("无权更新他人上传歌曲的信息");
        }
        if (dto.getName() != null) song.setName(dto.getName());
        if (dto.getDescription() != null) song.setDescription(dto.getDescription());
        if (!songManager.updateById(song)) {
            throw new BusinessException("歌曲信息更新失败");
        }
        SongVO songVO = new SongVO();
        BeanUtils.copyProperties(song, songVO);
        User uploader = userManager.getById(song.getUploaderId());
        if (uploader != null) {
            songVO.setUploaderName(uploader.getName());
        } else {
            songVO.setUploaderName("未知用户");
        }
log.info("更新歌曲信息成功");
        return songVO;
    }

    @Override
    public SongUploadPrepareVO prepareUpload(SongPrepareUploadDTO dto, Long userId) {
        log.info("请求准备上传歌曲，用户ID: {}", userId);
        User uploader = userManager.getById(userId);
        if (uploader == null) {
            throw new BusinessException("用户不存在");
        }
        String extension = MIME_TYPE_TO_EXTENSION_MAP.get(dto.getMimetype());
        if (extension == null) {
            throw new BusinessException("不支持该文件类型");
        }
        String name = dto.getName();
        if (name == null) {
            name = String.valueOf(IdWorker.getId());
        }
        Song song = new Song();
        song.setName(name);
        song.setDescription(dto.getDescription());
        song.setSize(0L);
        song.setDuration(null);
        song.setUploaderId(userId);
        song.setStatus(SongStatus.UPLOADING.name());
        if (!songManager.save(song)) {
            throw new BusinessException("歌曲信息保存失败");
        }
        String fileName = song.getId() + "." + extension;
        song.setFileName(fileName);
        if (!songManager.updateById(song)) {
            throw new BusinessException("文件名更新失败");
        }
        String uploadUrl = ossManager.generatePresignedPostUrl(fileName, Duration.ofMinutes(15));
        if (uploadUrl == null) {
            throw new BusinessException("生成上传URL失败");
        }
        SongUploadPrepareVO prepareVO = new SongUploadPrepareVO();
        prepareVO.setId(song.getId());
        prepareVO.setFileName(fileName);
        prepareVO.setUploadUrl(uploadUrl);
        log.info("准备上传歌曲成功，歌曲ID: {}, 文件名: {}", song.getId(), fileName);
        return prepareVO;
    }

    @Override
    public SongVO completeUpload(SongCompleteUploadDTO dto, Long userId) {
        log.info("请求完成上传歌曲，用户ID: {}, 歌曲ID: {}", userId, dto.getSongId());
        User uploader = userManager.getById(userId);
        if (uploader == null) {
            throw new BusinessException("用户不存在");
        }
        Song song = songManager.getById(dto.getSongId());
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        if (!SongStatus.UPLOADING.name().equals(song.getStatus())) {
            throw new BusinessException("歌曲状态异常");
        }
        if (!song.getUploaderId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (!ossManager.checkFileExists(song.getFileName())) {
            songManager.removeById(song);
            throw new BusinessException("上传文件不存在");
        }
        InputStream inputStream = ossManager.getFileInputStream(song.getFileName());
        if (inputStream == null) {
            throw new BusinessException("获取文件失败");
        }
        String actualMimeType;
        try {
            actualMimeType = TIKA.detect(inputStream);
        } catch (IOException e) {
            throw new BusinessException("无法获取文件类型", e);
        }
        try {
            inputStream.close();
        } catch (IOException e) {
            log.error("关闭文件流失败", e);
        }
        if (actualMimeType == null) {
            throw new BusinessException("无法获取文件类型");
        }
        String actualExtension = MIME_TYPE_TO_EXTENSION_MAP.get(actualMimeType);
        if (actualExtension == null) {
            songManager.removeById(song);
            ossManager.deleteFile(song.getFileName());
            throw new BusinessException("文件类型不支持");
        }
        String expectedExtension = song.getFileName().substring(song.getFileName().lastIndexOf('.') + 1);
        if (!actualExtension.equals(expectedExtension)) {
            songManager.removeById(song);
            ossManager.deleteFile(song.getFileName());
            throw new BusinessException("文件类型与后缀名不匹配");
        }
        Long fileSize = ossManager.getFileSize(song.getFileName());
        if (fileSize == null) {
            throw new BusinessException("获取文件大小失败");
        }
        inputStream = ossManager.getFileInputStream(song.getFileName());
        if (inputStream == null) {
            throw new BusinessException("获取文件失败");
        }
        Long duration = getAudioDurationInMills(inputStream);
        try {
            inputStream.close();
        } catch (IOException e) {
            log.error("关闭文件流失败", e);
        }
        if (duration == null) {
            log.warn("解析歌曲持续时长失败");
            duration = 0L;
        }
        song.setSize(fileSize);
        song.setDuration(duration);
        song.setStatus(SongStatus.NORMAL.name());
        if (!songManager.updateById(song)) {
            throw new BusinessException("歌曲信息更新失败");
        }
        SongVO songVO = new SongVO();
        BeanUtils.copyProperties(song, songVO);
        songVO.setUploaderName(uploader.getName());
        log.info("完成上传歌曲成功，歌曲ID: {}", song.getId());
        return songVO;
    }
}
