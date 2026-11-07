package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.system.dto.OrgQueryDTO;
import com.fm.module.system.dto.OrgSaveDTO;
import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.vo.OrgVO;

import java.util.List;

/**
 * 运营中心（租户）服务，仅平台管理员可维护。
 */
public interface SysOrgService extends IService<SysOrg> {

    Page<OrgVO> page(OrgQueryDTO query);

    List<OrgVO> options();

    OrgVO detail(Long id);

    Long create(OrgSaveDTO request);

    void update(Long id, OrgSaveDTO request);

    void delete(Long id);
}
