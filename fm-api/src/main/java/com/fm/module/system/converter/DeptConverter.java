package com.fm.module.system.converter;

import com.fm.module.system.entity.SysDept;
import com.fm.module.system.vo.DeptVO;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门对象转换。
 */
public final class DeptConverter {

    private DeptConverter() {
    }

    public static DeptVO toVO(SysDept dept) {
        if (dept == null) {
            return null;
        }
        DeptVO vo = new DeptVO();
        BeanUtils.copyProperties(dept, vo);
        return vo;
    }

    public static List<DeptVO> toTree(List<SysDept> depts) {
        Map<Long, DeptVO> nodeMap = new LinkedHashMap<>();
        for (SysDept dept : depts) {
            nodeMap.put(dept.getId(), toVO(dept));
        }
        List<DeptVO> roots = new ArrayList<>();
        for (DeptVO node : nodeMap.values()) {
            DeptVO parent = node.getParentId() == null ? null : nodeMap.get(node.getParentId());
            if (parent == null) {
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        return roots;
    }
}
