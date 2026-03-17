package top.enderliquid.audioflow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.enderliquid.audioflow.dto.request.loginlog.LoginLogPageDTO;
import top.enderliquid.audioflow.dto.response.PageVO;
import top.enderliquid.audioflow.dto.response.loginlog.LoginLogVO;
import top.enderliquid.audioflow.entity.LoginLog;
import top.enderliquid.audioflow.manager.LoginLogManager;
import top.enderliquid.audioflow.service.LoginLogService;

import java.util.ArrayList;
import java.util.List;

import static top.enderliquid.audioflow.common.constant.DefaultConstants.PAGE_DEFAULT_INDEX;
import static top.enderliquid.audioflow.common.constant.DefaultConstants.PAGE_DEFAULT_SIZE;

@Slf4j
@Service
public class LoginLogServiceImpl implements LoginLogService {

    @Autowired
    private LoginLogManager loginLogManager;

    @Override
    public PageVO<LoginLogVO> page(Long userId, LoginLogPageDTO dto) {
        log.info("查询登录流水，用户ID: {}", userId);

        Long pageIndex = dto.getPageIndex() != null ? dto.getPageIndex() : PAGE_DEFAULT_INDEX;
        Long pageSize = dto.getPageSize() != null ? dto.getPageSize() : PAGE_DEFAULT_SIZE;

        Page<LoginLog> page = loginLogManager.pageByUserId(
                userId,
                pageIndex,
                pageSize
        );

        List<LoginLog> logList = page.getRecords();
        List<LoginLogVO> logVOList = new ArrayList<>();
        if (logList != null && !logList.isEmpty()) {
            for(LoginLog log : logList){
                if (log == null) continue;
                LoginLogVO logVO = new LoginLogVO();
                BeanUtils.copyProperties(log, logVO);
                logVOList.add(logVO);
            }
        }

        PageVO<LoginLogVO> pageVO = new PageVO<>();
        pageVO.setList(logVOList);
        pageVO.setPageIndex(page.getCurrent());
        pageVO.setPageSize(page.getSize());
        pageVO.setTotal(page.getTotal());
        log.info("登录流水查询成功，共 {} 条", page.getTotal());
        return pageVO;
    }
}