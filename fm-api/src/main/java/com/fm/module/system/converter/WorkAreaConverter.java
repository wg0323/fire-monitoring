package com.fm.module.system.converter;

import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.vo.WorkAreaVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 工作区对象转换。
 */
public final class WorkAreaConverter {

    private WorkAreaConverter() {
    }

    public static WorkAreaVO toVO(SysWorkArea workArea) {
        if (workArea == null) {
            return null;
        }
        WorkAreaVO vo = new WorkAreaVO();
        BeanUtils.copyProperties(workArea, vo);
        return vo;
    }

    public static List<WorkAreaVO> toVOList(List<SysWorkArea> workAreas) {
        return workAreas.stream().map(WorkAreaConverter::toVO).collect(Collectors.toList());
    }
}
