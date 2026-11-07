package com.fm.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.system.dto.DeptSaveDTO;
import com.fm.module.system.entity.SysDept;
import com.fm.module.system.vo.DeptVO;

import java.util.List;

/**
 * 部门服务：按租户隔离。
 */
public interface SysDeptService extends IService<SysDept> {

    List<DeptVO> tree(Long tenantId);

    Long create(DeptSaveDTO request);

    void update(Long id, DeptSaveDTO request);

    void delete(Long id);
}
