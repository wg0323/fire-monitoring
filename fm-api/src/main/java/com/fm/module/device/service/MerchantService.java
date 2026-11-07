package com.fm.module.device.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.fm.module.device.dto.MerchantQueryDTO;
import com.fm.module.device.dto.MerchantSaveDTO;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.vo.MerchantDetailVO;
import com.fm.module.device.vo.MerchantOptionVO;
import com.fm.module.device.vo.MerchantVO;

import java.util.List;

/**
 * 商户服务。
 */
public interface MerchantService extends IService<Merchant> {

    Page<MerchantVO> page(MerchantQueryDTO query);

    MerchantDetailVO detail(Long id);

    Long create(MerchantSaveDTO request);

    void update(MerchantSaveDTO request);

    void delete(Long id);

    void updateStatus(Long id, Integer status);

    void bindWorkAreas(Long id, List<Long> workAreaIds);

    void bindDevices(Long id, List<Long> deviceIds);

    void unbindDevice(Long id, Long deviceId);

    List<Merchant> options(Long tenantId);

    /**
     * 小程序企业下拉选项：按登录角色收窄可见企业，最多返回固定条数。
     *
     * @param keyword 企业名称关键字，可为空
     * @return 商户下拉选项
     */
    List<MerchantOptionVO> merchantOptions(String keyword);
}
