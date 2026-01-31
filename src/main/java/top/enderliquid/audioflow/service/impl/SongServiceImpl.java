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
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.helpers.DefaultHandler;
import top.enderliquid.audioflow.common.exception.BusinessException;
import top.enderliquid.audioflow.dto.bo.SongBO;
import top.enderliquid.audioflow.dto.request.SongPageDTO;
import top.enderliquid.audioflow.dto.response.CommonPageVO;
import top.enderliquid.audioflow.dto.response.SongVO;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.FileManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.SongService;

import java.io.IOException;
import java.io.InputStream;
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
    private FileManager fileManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    private static final Tika TIKA = new Tika();
    private static final Parser TIKA_PARSER = new AutoDetectParser();

    private static final char[] ILLEGAL_FILE_NAME_CHARS = new char[]
            {'\\', '/', ':', '*', '?', '"', '\'', '<', '>', '|'};

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
    public SongVO saveSong(MultipartFile file, Long userId) {
        log.info("用户ID为 {} 的用户请求上传歌曲", userId);
        // 检测文件是否为空
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        // 检查用户是否存在
        User uploader = userManager.getById(userId);
        if (uploader == null) {
            throw new BusinessException("用户不存在");
        }
        /*
         Opera浏览器的MultipartFile对象
         调用getOriginalFilename()可能返回带路径的值，
         因此需要显式分离出文件全称。
         注意，MultipartFile对象调用getOriginalFilename()可能返回null。
        */
        String originFileName = StringUtils.getFilename(file.getOriginalFilename());
        String originName;
        // 提前生成歌曲Id
        long songId = IdWorker.getId();
        // 获取原始文件名
        String defaultOriginName = String.valueOf(songId);
        originName = getOriginNameFromOriginFileName(originFileName, defaultOriginName);
        if (originName.equals(defaultOriginName)) {
            log.warn("文件名非法，使用歌曲ID作为原始文件名");
        }
        // 忽略原始文件扩展名，通过Magic Number推断文件类型
        String mimeType;
        try (InputStream inputStream = file.getInputStream()) {
            mimeType = TIKA.detect(inputStream);
        } catch (IOException e) {
            throw new BusinessException("无法获取文件类型", e);
        }
        String extension = MIME_TYPE_TO_EXTENSION_MAP.get(mimeType);
        if (extension == null) {
            throw new BusinessException("不支持的文件类型");
        }
        log.info("歌曲文件通过检验，原始文件名: {}，文件扩展名: {}", originName, extension);
        String fileName = songId + "." + extension;
        // 保存歌曲文件
        String sourceType;
        try (InputStream inputStream = file.getInputStream()) {
            sourceType = fileManager.save(fileName, inputStream);
        } catch (IOException e) {
            sourceType = null;
        }
        if (sourceType == null) {
            throw new BusinessException("歌曲文件保存失败");
        }
        log.info("歌曲文件保存成功，保存文件名: {}", fileName);
        // 保存歌曲信息
        Song song = new Song();
        song.setId(songId);

        //TODO:重写名称与描述解析逻辑
        song.setName(originName);
        song.setDescription("");

        song.setFileName(fileName);
        song.setSourceType(sourceType);
        song.setSize(file.getSize());
        song.setUploaderId(userId);
        long duration = getAudioDurationInMills(file);
        if (duration == 0) {
            log.warn("解析歌曲持续时长失败");
        }
        song.setDuration(duration);
        try {
            transactionTemplate.execute((status -> {
                // 再次检测用户是否存在（防御式编程，虽然数据库应该有外键约束）
                if (!userManager.existsById(userId)) {
                    throw new BusinessException("用户不存在");
                }
                // 保存歌曲信息到数据库
                if (!songManager.save(song)) {
                    throw new BusinessException("歌曲信息保存失败");
                }
                return null;
            }));
        } catch (BusinessException e) {
            if (!fileManager.delete(fileName, sourceType)) {
                log.error("删除已保存的歌曲文件失败: {}", fileName);
            }
            throw e;
        }
        log.info("歌曲信息保存成功");
        // 返回视图
        SongVO songVO = new SongVO();
        BeanUtils.copyProperties(song, songVO);
        songVO.setUploaderName(uploader.getName());
        songVO.setFileUrl(fileManager.getUrl(song.getFileName(),song.getSourceType()));
        return songVO;
    }

    // 从文件全称获取原始文件名
    private String getOriginNameFromOriginFileName(String originFileName, String defaultOriginName) {
        if (originFileName == null) {
            return defaultOriginName;
        }
        originFileName = originFileName.trim();
        if (originFileName.isEmpty()) {
            return defaultOriginName;
        }
        String originName;
        // 获取第一个'.'的下标
        int index = originFileName.lastIndexOf('.');
        if (index == -1) {
            // 文件全称中没有'.'
            originName = originFileName;
        } else {
            originName = originFileName.substring(0, index);
        }
        originName = originName.trim();
        if (originName.isEmpty()) {
            return defaultOriginName;
        }
        if (originName.length() > 128) {
            return defaultOriginName;
        }
        // 去除非法字符
        for (char c : ILLEGAL_FILE_NAME_CHARS) {
            originName = originName.replace(c, ' ');
        }
        return originName;
    }

    private long getAudioDurationInMills(MultipartFile file) {
        //TODO:方法无效，需要调试
        DefaultHandler handler = new DefaultHandler();
        Metadata metadata = new Metadata();
        ParseContext parseContext = new ParseContext();
        String durationStr;
        try (InputStream inputStream = file.getInputStream()) {
            TIKA_PARSER.parse(inputStream, handler, metadata, parseContext);
            durationStr = metadata.get(XMPDM.DURATION);
        } catch (Exception e) {
            return 0;
        }
        if (durationStr == null || durationStr.isEmpty()) {
            return 0;
        }
        double durationSeconds;
        try {
            durationSeconds = Double.parseDouble(durationStr);
        } catch (NumberFormatException e) {
            return 0;
        }
        return Math.round(durationSeconds * 1000);
    }

    @Override
    public CommonPageVO<SongVO> pageSongsByUploaderKeywordAndSongKeyword(SongPageDTO dto) {
        String uploaderKeyword = dto.getUploaderKeyword();
        String songKeyword = dto.getSongKeyword();
        Boolean isAsc = dto.getIsAsc();
        if (isAsc == null) isAsc = false;
        Long pageNum = dto.getPageNum();
        if (pageNum == null) pageNum = 1L;
        Long pageSize = dto.getPageSize();
        if (pageSize == null) pageSize = 10L;
        IPage<SongBO> songBOPage = songManager.pageByUploaderKeywordAndSongKeyword(
                uploaderKeyword,
                songKeyword,
                isAsc,
                pageNum,
                pageSize
        );
        List<SongBO> songBOList = songBOPage.getRecords();
        List<SongVO> songVOList = new ArrayList<>();
        if (songBOList != null && !songBOList.isEmpty()) {
            for (SongBO songBO : songBOList) {
                if (songBO == null) continue;
                SongVO songVO = new SongVO();
                BeanUtils.copyProperties(songBO, songVO);
                songVO.setFileUrl(fileManager.getUrl(songBO.getFileName(),songBO.getSourceType()));
                songVOList.add(songVO);
            }
        }
        CommonPageVO<SongVO> pageVO = new CommonPageVO<>();
        pageVO.setList(songVOList);
        pageVO.setNum(pageNum);
        pageVO.setSize(pageSize);
        pageVO.setTotal(songBOPage.getTotal());
        return pageVO;
    }

    // 普通用户: 验证歌曲所有权
    @Override
    public void removeSong(Long songId, Long userId) {
        log.info("用户ID为 {} 的用户请求删除歌曲ID为 {} 的歌曲", userId, songId);
        Song song = songManager.getById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        if (!song.getUploaderId().equals(userId)) {
            throw new BusinessException("无权删除他人上传的歌曲");
        }
        doRemoveSong(song);
    }

    // 管理员: 强制删除歌曲
    @Override
    public void removeSongForce(Long songId) {
        log.info("请求强制删除ID为 {} 的歌曲", songId);
        Song song = songManager.getById(songId);
        if (song == null) {
            throw new BusinessException("歌曲不存在");
        }
        doRemoveSong(song);
    }

    private void doRemoveSong(Song song) {
        if (!songManager.removeById(song)) {
            log.error("删除歌曲信息失败");
            throw new BusinessException("删除歌曲信息失败");
        }
        log.info("删除歌曲信息成功");
        String fileName = song.getFileName();
        if (!fileManager.delete(fileName, song.getSourceType())) {
            log.error("删除歌曲文件失败");
        } else {
            log.info("删除歌曲文件成功");
        }
    }
}
