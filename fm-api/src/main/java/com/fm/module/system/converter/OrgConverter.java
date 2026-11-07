package com.fm.module.system.converter;

import com.fm.module.system.entity.SysOrg;
import com.fm.module.system.vo.OrgVO;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 运营中心（租户）对象转换。
 */
public final class OrgConverter {

    private OrgConverter() {
    }

    public static OrgVO toVO(SysOrg org) {
        if (org == null) {
            return null;
        }
        OrgVO vo = new OrgVO();
        BeanUtils.copyProperties(org, vo);
        return vo;
    }

    public static List<OrgVO> toVOList(List<SysOrg> orgs) {
        return orgs.stream().map(OrgConverter::toVO).collect(Collectors.toList());
    }
}
