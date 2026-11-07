package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.system.dto.WorkAreaQueryDTO;
import com.fm.module.system.dto.WorkAreaSaveDTO;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.vo.UserVO;
import com.fm.module.system.vo.WorkAreaVO;

import java.util.List;

/**
 * 工作区服务：平台跨租户，租户管理员限本租户。
 */
public interface SysWorkAreaService extends IService<SysWorkArea> {

    Page<WorkAreaVO> page(WorkAreaQueryDTO query);

    List<WorkAreaVO> options(Long tenantId);

    WorkAreaVO detail(Long id);

    Long create(WorkAreaSaveDTO request);

    void update(Long id, WorkAreaSaveDTO request);

    void delete(Long id);

    void bindMerchants(Long id, List<Long> merchantIds);

    void bindUsers(Long id, List<Long> userIds);

    List<UserVO> users(Long id);

    List<Long> merchantIds(Long id);
}
