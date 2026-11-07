package com.fm.module.device.converter;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.vo.MerchantDetailVO;
import com.fm.module.device.vo.MerchantOptionVO;
import com.fm.module.device.vo.MerchantVO;
import com.fm.module.device.vo.WorkAreaBriefVO;
import com.fm.module.system.entity.SysWorkArea;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 商户对象转换。
 */
public final class MerchantConverter {

    private MerchantConverter() {
    }

    public static MerchantVO toVO(Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        MerchantVO vo = new MerchantVO();
        BeanUtils.copyProperties(merchant, vo);
        return vo;
    }

    public static MerchantDetailVO toDetailVO(Merchant merchant) {
        if (merchant == null) {
            return null;
        }
        MerchantDetailVO vo = new MerchantDetailVO();
        BeanUtils.copyProperties(merchant, vo);
        return vo;
    }

    public static List<MerchantVO> toVOList(List<Merchant> merchants) {
        return merchants.stream().map(MerchantConverter::toVO).collect(Collectors.toList());
    }

    public static Page<MerchantVO> toVOPage(Page<Merchant> page) {
        Page<MerchantVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(toVOList(page.getRecords()));
        return result;
    }

    public static List<MerchantOptionVO> toOptionVOList(List<Merchant> merchants) {
        return merchants.stream().map(merchant -> {
            MerchantOptionVO vo = new MerchantOptionVO();
            BeanUtils.copyProperties(merchant, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    public static WorkAreaBriefVO toWorkAreaBriefVO(SysWorkArea workArea) {
        if (workArea == null) {
            return null;
        }
        WorkAreaBriefVO vo = new WorkAreaBriefVO();
        BeanUtils.copyProperties(workArea, vo);
        return vo;
    }
}
