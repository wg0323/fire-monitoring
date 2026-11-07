package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.system.dto.LoginLogQueryDTO;
import com.fm.module.system.vo.LoginLogVO;

/**
 * 登录日志查询。
 */
public interface LoginLogService {

    Page<LoginLogVO> page(LoginLogQueryDTO query);
}
