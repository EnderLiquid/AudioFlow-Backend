package top.enderliquid.audioflow.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.enderliquid.audioflow.common.enums.Role;
import top.enderliquid.audioflow.dto.request.song.SongBatchDeleteDTO;
import top.enderliquid.audioflow.dto.request.song.SongBatchPrepareDTO;
import top.enderliquid.audioflow.dto.request.song.SongPrepareUploadDTO;
import top.enderliquid.audioflow.dto.response.BatchFailureItem;
import top.enderliquid.audioflow.dto.response.SongBatchResultVO;
import top.enderliquid.audioflow.dto.response.SongUploadPrepareVO;
import top.enderliquid.audioflow.entity.Song;
import top.enderliquid.audioflow.entity.User;
import top.enderliquid.audioflow.manager.OSSManager;
import top.enderliquid.audioflow.manager.SongManager;
import top.enderliquid.audioflow.manager.UserManager;
import top.enderliquid.audioflow.service.impl.SongServiceImpl;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SongBatchServiceTest {

    @Mock
    private UserManager userManager;

    @Mock
    private SongManager songManager;

    @Mock
    private OSSManager ossManager;

    @InjectMocks
    private SongServiceImpl songService;

    private User testUser;
    private final Long testUserId = 1L;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(testUserId);
        testUser.setName("测试用户");
        testUser.setRole(Role.USER);
    }

    @Test
    @DisplayName("批量准备上传-成功")
    void batchPrepareUpload_success() {
        when(userManager.getById(testUserId)).thenReturn(testUser);
        when(songManager.save(any(Song.class))).thenReturn(true);
        when(ossManager.generatePresignedPutUrl(anyString(), anyString())).thenReturn("https://test.url");

        List<SongPrepareUploadDTO> songs = new ArrayList<>();
        songs.add(new SongPrepareUploadDTO("audio/mpeg", "歌曲1", "描述1"));
        songs.add(new SongPrepareUploadDTO("audio/mpeg", "歌曲2", "描述2"));

        SongBatchPrepareDTO dto = new SongBatchPrepareDTO(songs);

        SongBatchResultVO<SongUploadPrepareVO> result = songService.batchPrepareUpload(dto, testUserId);

        assertEquals(2, result.getSuccessCount());
        assertEquals(0, result.getFailureCount());
        verify(songManager, times(2)).save(any(Song.class));
    }

    @Test
    @DisplayName("批量准备上传-部分失败")
    void batchPrepareUpload_partialFailure() {
        when(userManager.getById(testUserId)).thenReturn(testUser);
        when(songManager.save(any(Song.class))).thenReturn(true);
        when(ossManager.generatePresignedPutUrl(anyString(), anyString())).thenReturn("https://test.url");

        List<SongPrepareUploadDTO> songs = new ArrayList<>();
        songs.add(new SongPrepareUploadDTO("audio/mpeg", "歌曲1", "描述1"));
        songs.add(new SongPrepareUploadDTO("invalid/type", "歌曲2", "描述2"));

        SongBatchPrepareDTO dto = new SongBatchPrepareDTO(songs);

        SongBatchResultVO<SongUploadPrepareVO> result = songService.batchPrepareUpload(dto, testUserId);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        BatchFailureItem failureItem = result.getFailureList().get(0);
        assertEquals("不支持该文件类型", failureItem.getReason());
    }

    @Test
    @DisplayName("批量删除-部分成功")
    void batchRemoveSongs_partialSuccess() {
        Song song1 = new Song();
        song1.setId(1L);
        song1.setUploaderId(testUserId);
        song1.setFileName("1.mp3");

        Song song2 = new Song();
        song2.setId(2L);
        song2.setUploaderId(999L);
        song2.setFileName("2.mp3");

        when(userManager.getById(testUserId)).thenReturn(testUser);
        when(songManager.getById(1L)).thenReturn(song1);
        when(songManager.getById(2L)).thenReturn(song2);
        when(songManager.removeById(any(Song.class))).thenReturn(true);
        when(ossManager.deleteFile(anyString())).thenReturn(true);

        List<Long> songIds = List.of(1L, 2L);
        SongBatchDeleteDTO dto = new SongBatchDeleteDTO(songIds);

        SongBatchResultVO<Long> result = songService.batchRemoveSongs(dto, testUserId);

        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        BatchFailureItem failureItem = result.getFailureList().get(0);
        assertEquals("无权删除他人上传的歌曲", failureItem.getReason());
    }
}