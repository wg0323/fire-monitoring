package com.fm.module.monitor.service;

import com.fm.module.monitor.vo.ArchiveMerchantVO;

import java.util.List;

/**
 * 电子档案：水系统 / 电气火灾的商户楼宇树。
 */
public interface ArchiveService {

    String CATEGORY_WATER = "water";
    String CATEGORY_ELECTRIC = "electric";

    /**
     * 按设备类型构建设备档案树：商户 → 楼宇 → 楼层 → 设备。
     * 未分配楼宇或楼层的设备挂在上一级节点的 devices 上。
     *
     * @param category water / electric
     * @return 商户节点列表，无数据返回空列表
     */
    List<ArchiveMerchantVO> tree(String category);
}
