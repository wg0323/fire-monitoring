package com.fm.module.device.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fm.common.context.TenantContext;
import com.fm.common.exception.BizException;
import com.fm.module.device.converter.BuildingConverter;
import com.fm.module.device.converter.DeviceConverter;
import com.fm.module.device.converter.FloorConverter;
import com.fm.module.device.converter.MerchantConverter;
import com.fm.module.device.dto.MerchantQueryDTO;
import com.fm.module.device.dto.MerchantSaveDTO;
import com.fm.module.device.entity.Device;
import com.fm.module.device.entity.DictDeviceType;
import com.fm.module.device.entity.DictMerchantType;
import com.fm.module.device.entity.Merchant;
import com.fm.module.device.entity.MerchantBuilding;
import com.fm.module.device.entity.MerchantFloor;
import com.fm.module.device.mapper.DeviceMapper;
import com.fm.module.device.mapper.DictDeviceTypeMapper;
import com.fm.module.device.mapper.DictMerchantTypeMapper;
import com.fm.module.device.mapper.MerchantBuildingMapper;
import com.fm.module.device.mapper.MerchantFloorMapper;
import com.fm.module.device.mapper.MerchantMapper;
import com.fm.module.device.service.MerchantService;
import com.fm.module.device.vo.BuildingNodeVO;
import com.fm.module.device.vo.FloorNodeVO;
import com.fm.module.device.vo.MerchantDetailVO;
import com.fm.module.device.vo.MerchantDeviceBriefVO;
import com.fm.module.device.vo.MerchantOptionVO;
import com.fm.module.device.vo.MerchantVO;
import com.fm.module.device.vo.WorkAreaBriefVO;
import com.fm.module.system.entity.SysRole;
import com.fm.module.system.entity.SysUser;
import com.fm.module.system.entity.SysUserMerchant;
import com.fm.module.system.entity.SysUserRole;
import com.fm.module.system.entity.SysWorkArea;
import com.fm.module.system.entity.SysWorkAreaMerchant;
import com.fm.module.system.mapper.SysRoleMapper;
import com.fm.module.system.mapper.SysUserMapper;
import com.fm.module.system.mapper.SysUserMerchantMapper;
import com.fm.module.system.mapper.SysUserRoleMapper;
import com.fm.module.system.mapper.SysWorkAreaMapper;
import com.fm.module.system.mapper.SysWorkAreaMerchantMapper;
import com.fm.module.system.service.DataScopeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 商户服务：列表补充工作区/设备数，详情聚合楼宇楼层与设备。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantServiceImpl extends ServiceImpl<MerchantMapper, Merchant> implements MerchantService {

    private static final int KIND_NINE_SMALL = 1;
    private static final int KIND_SOCIAL_UNIT = 2;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;
    private static final int NAME_MAX_LENGTH = 64;
    private static final int EMPLOYEE_MAX = 99999;
    /** 小程序企业下拉最多返回的条数 */
    private static final int OPTION_LIMIT = 50;
    /** 商户账号取不到所属商户时使用的占位 ID，保证查询返回空集而非全量 */
    private static final Long EMPTY_MERCHANT_ID = -1L;
    /** 与 miniapp 端 MpConstants.USER_TYPE_MERCHANT 同值，避免 service 反向依赖 miniapp 包 */
    private static final String USER_TYPE_MERCHANT = "MERCHANT";
    private static final String MERCHANT_ROLE_KEY = "MERCHANT";
    private static final String MERCHANT_USER_NAME_PREFIX = "merchant_";
    private static final String OWNER_USER_NAME_PREFIX = "owner_";
    private static final String DEFAULT_ACCOUNT_PASSWORD = "123456";
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+\\-\\s]{6,24}$");
    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final DataScopeService dataScopeService;
    private final MerchantBuildingMapper buildingMapper;
    private final MerchantFloorMapper floorMapper;
    private final DeviceMapper deviceMapper;
    private final DictMerchantTypeMapper merchantTypeMapper;
    private final DictDeviceTypeMapper deviceTypeMapper;
    private final SysWorkAreaMapper workAreaMapper;
    private final SysWorkAreaMerchantMapper workAreaMerchantMapper;
    private final SysUserMapper userMapper;
    private final SysUserMerchantMapper userMerchantMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Page<MerchantVO> page(MerchantQueryDTO query) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(Merchant::getName, query.getKeyword())
                    .or().like(Merchant::getOwnerName, query.getKeyword())
                    .or().like(Merchant::getPhone, query.getKeyword()));
        }
        if (StringUtils.hasText(query.getType())) {
            wrapper.eq(Merchant::getType, query.getType());
        }
        if (query.getMerchantKind() != null) {
            wrapper.eq(Merchant::getMerchantKind, query.getMerchantKind());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Merchant::getStatus, query.getStatus());
        }
        if (query.getWorkAreaId() != null) {
            List<Long> merchantIds = merchantIdsOfWorkArea(query.getWorkAreaId());
            if (merchantIds.isEmpty()) {
                return new Page<>(query.getPageNum(), query.getPageSize(), 0);
            }
            wrapper.in(Merchant::getId, merchantIds);
        }
        wrapper.orderByDesc(Merchant::getCreateTime);
        Page<Merchant> page = new Page<>(query.getPageNum(), query.getPageSize());
        Page<Merchant> entityPage;
        if (!dataScopeService.isPlatform()) {
            entityPage = baseMapper.selectPage(page, wrapper);
        } else {
            List<Long> tenantIds = dataScopeService.authorizedTenantIds();
            if (tenantIds.isEmpty()) {
                return new Page<>(query.getPageNum(), query.getPageSize(), 0);
            }
            wrapper.in(Merchant::getTenantId, tenantIds);
            entityPage = scoped(() -> baseMapper.selectPage(page, wrapper));
        }
        Page<MerchantVO> result = MerchantConverter.toVOPage(entityPage);
        enrich(result.getRecords());
        return result;
    }

    @Override
    public MerchantDetailVO detail(Long id) {
        Merchant merchant = requireMerchant(id);
        MerchantDetailVO vo = MerchantConverter.toDetailVO(merchant);
        enrich(Collections.singletonList(vo));
        List<WorkAreaBriefVO> workAreas = workAreasOfMerchant(id);
        vo.setWorkAreas(workAreas);
        vo.setBuildings(buildingsOfMerchant(merchant));
        vo.setDevices(devicesOfMerchant(merchant));
        return vo;
    }

    @Override
    @Transactional
    public Long create(MerchantSaveDTO request) {
        validate(request);
        Merchant merchant = toEntity(request);
        merchant.setTenantId(TenantContext.getTenantId());
        merchant.setStatus(request.getStatus() == null ? STATUS_ENABLED : request.getStatus());
        baseMapper.insert(merchant);
        ensureMerchantAccount(merchant);
        return merchant.getId();
    }

    @Override
    @Transactional
    public void update(MerchantSaveDTO request) {
        if (request.getId() == null) {
            throw new BizException("商户 id 不能为空");
        }
        requireMerchant(request.getId());
        validate(request);
        LambdaUpdateWrapper<Merchant> wrapper = new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, request.getId())
                .set(Merchant::getName, request.getName())
                .set(Merchant::getType, request.getType())
                .set(Merchant::getMerchantKind, request.getMerchantKind())
                .set(Merchant::getLng, request.getLng())
                .set(Merchant::getLat, request.getLat())
                .set(Merchant::getAddr, request.getAddr())
                .set(Merchant::getArea, request.getArea())
                .set(Merchant::getOwnerName, request.getOwnerName())
                .set(Merchant::getPhone, request.getPhone())
                .set(Merchant::getBusinessScope, request.getBusinessScope())
                .set(Merchant::getBusinessContent, request.getBusinessContent())
                .set(Merchant::getFloorage, request.getFloorage())
                .set(Merchant::getEmployeeNum, request.getEmployeeNum())
                .set(Merchant::getAllowNum, request.getAllowNum())
                .set(Merchant::getBusinessHours, request.getBusinessHours())
                .set(Merchant::getRoundEnv, request.getRoundEnv())
                .set(Merchant::getPlaceFlag, request.getPlaceFlag())
                .set(Merchant::getContact1, request.getContact1())
                .set(Merchant::getPhone1, request.getPhone1())
                .set(Merchant::getContact2, request.getContact2())
                .set(Merchant::getPhone2, request.getPhone2())
                .set(Merchant::getBusinessLicense, request.getBusinessLicense())
                .set(Merchant::getFireRemark, request.getFireRemark())
                .set(Merchant::getPlacePic1, request.getPlacePic1())
                .set(Merchant::getPlacePic2, request.getPlacePic2())
                .set(Merchant::getPlacePic3, request.getPlacePic3())
                .set(Merchant::getRemark, request.getRemark())
                .set(Merchant::getInsureFlag, request.getInsureFlag())
                .set(Merchant::getStatus, request.getStatus());
        baseMapper.update(null, wrapper);
        Merchant updated = getById(request.getId());
        if (updated != null) {
            ensureMerchantAccount(updated);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Merchant merchant = requireMerchant(id);
        Long deviceCount = scoped(() -> deviceMapper.selectCount(new LambdaQueryWrapper<Device>()
                .eq(Device::getMerchantId, merchant.getId())));
        if (deviceCount != null && deviceCount > 0) {
            throw new BizException("商户下仍有设备，请先解绑后再删除");
        }
        scopedVoid(() -> workAreaMerchantMapper.delete(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getMerchantId, id)));
        baseMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        requireMerchant(id);
        if (status == null || (status != STATUS_DISABLED && status != STATUS_ENABLED)) {
            throw new BizException("商户状态不合法");
        }
        baseMapper.update(null, new LambdaUpdateWrapper<Merchant>()
                .eq(Merchant::getId, id)
                .set(Merchant::getStatus, status));
        syncMerchantAccountStatus(id, status);
    }

    @Override
    @Transactional
    public void bindWorkAreas(Long id, List<Long> workAreaIds) {
        Merchant merchant = requireMerchant(id);
        List<Long> targetIds = distinctIds(workAreaIds);
        validateWorkAreas(merchant.getTenantId(), targetIds);
        scopedVoid(() -> workAreaMerchantMapper.delete(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                .eq(SysWorkAreaMerchant::getMerchantId, id)));
        for (Long workAreaId : targetIds) {
            SysWorkAreaMerchant relation = new SysWorkAreaMerchant();
            relation.setTenantId(merchant.getTenantId());
            relation.setWorkAreaId(workAreaId);
            relation.setMerchantId(id);
            scopedVoid(() -> workAreaMerchantMapper.insert(relation));
        }
    }

    @Override
    @Transactional
    public void bindDevices(Long id, List<Long> deviceIds) {
        Merchant merchant = requireMerchant(id);
        List<Long> targetIds = distinctIds(deviceIds);
        if (targetIds.isEmpty()) {
            return;
        }
        List<Device> devices = scoped(() -> deviceMapper.selectBatchIds(targetIds));
        if (devices.size() != targetIds.size()) {
            throw new BizException("部分设备不存在或无权操作");
        }
        boolean tenantMismatch = devices.stream()
                .anyMatch(device -> !merchant.getTenantId().equals(device.getTenantId()));
        if (tenantMismatch) {
            throw new BizException("设备与商户不属于同一运营中心");
        }
        for (Device device : devices) {
            boolean merchantChanged = !merchant.getId().equals(device.getMerchantId());
            LambdaUpdateWrapper<Device> wrapper = new LambdaUpdateWrapper<Device>()
                    .eq(Device::getId, device.getId())
                    .set(Device::getMerchantId, merchant.getId());
            if (merchantChanged) {
                wrapper.set(Device::getLocationType, Device.LOCATION_NONE)
                        .set(Device::getBuildingId, null)
                        .set(Device::getFloorId, null)
                        .set(Device::getFloorX, null)
                        .set(Device::getFloorY, null);
            }
            deviceMapper.update(null, wrapper);
        }
    }

    @Override
    @Transactional
    public void unbindDevice(Long id, Long deviceId) {
        Merchant merchant = requireMerchant(id);
        Device device = scoped(() -> deviceMapper.selectById(deviceId));
        if (device == null || !merchant.getId().equals(device.getMerchantId())) {
            throw new BizException("设备未绑定该商户");
        }
        deviceMapper.update(null, new LambdaUpdateWrapper<Device>()
                .eq(Device::getId, deviceId)
                .set(Device::getMerchantId, null)
                .set(Device::getLocationType, Device.LOCATION_NONE)
                .set(Device::getBuildingId, null)
                .set(Device::getFloorId, null)
                .set(Device::getFloorX, null)
                .set(Device::getFloorY, null));
    }

    @Override
    public List<Merchant> options(Long tenantId) {
        Long targetTenant = dataScopeService.isPlatform() ? tenantId : dataScopeService.tenantId();
        if (targetTenant == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getTenantId, targetTenant)
                .eq(Merchant::getStatus, STATUS_ENABLED)
                .orderByAsc(Merchant::getName)
                .last("LIMIT 200");
        if (dataScopeService.isPlatform()) {
            return scoped(() -> baseMapper.selectList(wrapper));
        }
        return baseMapper.selectList(wrapper);
    }

    @Override
    public List<MerchantOptionVO> merchantOptions(String keyword) {
        LambdaQueryWrapper<Merchant> wrapper = new LambdaQueryWrapper<Merchant>()
                .eq(Merchant::getStatus, STATUS_ENABLED)
                .likeRight(StringUtils.hasText(keyword), Merchant::getName, keyword)
                .orderByAsc(Merchant::getName)
                .last("LIMIT " + OPTION_LIMIT);
        if (USER_TYPE_MERCHANT.equals(TenantContext.getUserType())) {
            Long merchantId = TenantContext.getMerchantId();
            wrapper.eq(Merchant::getId, merchantId == null ? EMPTY_MERCHANT_ID : merchantId);
        } else if (SysRole.SCOPE_WORK_AREA.equals(TenantContext.getDataScope())) {
            List<Long> merchantIds = dataScopeService.workAreaMerchantIds(StpUtil.getLoginIdAsLong());
            if (merchantIds.isEmpty()) {
                return Collections.emptyList();
            }
            wrapper.in(Merchant::getId, merchantIds);
        }
        return MerchantConverter.toOptionVOList(baseMapper.selectList(wrapper));
    }

    @Override
    public Merchant getById(Serializable id) {
        if (!dataScopeService.isPlatform()) {
            return super.getById(id);
        }
        return scoped(() -> super.getById(id));
    }

    /**
     * 商户两类使用人：第一联系人（phone1）与业主（phone），各开一个 MERCHANT 账号；
     * 手机号已被同租户账号占用时只追加门店关联，不重复开户。
     */
    private void ensureMerchantAccount(Merchant merchant) {
        ensureAccount(merchant, merchant.getPhone1(), contactName(merchant),
                MERCHANT_USER_NAME_PREFIX, "第一联系人");
        ensureAccount(merchant, merchant.getPhone(), ownerName(merchant),
                OWNER_USER_NAME_PREFIX, "业主");
    }

    private void ensureAccount(Merchant merchant, String phone, String realName,
                               String userNamePrefix, String userLabel) {
        if (!StringUtils.hasText(phone) || !MOBILE_PATTERN.matcher(phone).matches()) {
            log.warn("商户 {} 未配置有效的{}手机号，跳过小程序账号开户", merchant.getId(), userLabel);
            return;
        }
        SysUser account = primaryAccount(merchant.getId(), userNamePrefix);
        if (account == null) {
            createAccount(merchant, phone, realName, userNamePrefix, userLabel);
            return;
        }
        syncAccount(account, merchant, phone, realName);
    }

    /**
     * 主账号：merchant_id 指向该商户，按登录名前缀区分第一联系人与业主。
     */
    private SysUser primaryAccount(Long merchantId, String userNamePrefix) {
        return scoped(() -> userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getMerchantId, merchantId)
                        .eq(SysUser::getUserType, USER_TYPE_MERCHANT)
                        .orderByAsc(SysUser::getId)))
                .stream()
                .filter(user -> user.getUserName() != null && user.getUserName().startsWith(userNamePrefix))
                .findFirst().orElse(null);
    }

    private void createAccount(Merchant merchant, String phone, String realName,
                               String userNamePrefix, String userLabel) {
        SysUser holder = accountByPhone(phone);
        if (holder != null) {
            if (merchant.getTenantId() != null && merchant.getTenantId().equals(holder.getTenantId())) {
                attachMerchant(holder.getId(), merchant);
                log.info("商户 {} 的{}手机号 {} 已有账号 {}，已追加门店关联",
                        merchant.getId(), userLabel, phone, holder.getUserName());
            } else {
                log.warn("商户 {} 的{}手机号 {} 属于其他运营中心账号，跳过开户",
                        merchant.getId(), userLabel, phone);
            }
            return;
        }
        if (!userMapper.selectByPhoneIncludeDeleted(phone).isEmpty()) {
            log.warn("商户 {} 的{}手机号 {} 已被历史账号占用，跳过开户",
                    merchant.getId(), userLabel, phone);
            return;
        }
        String userName = userNamePrefix + merchant.getId();
        if (userMapper.countByUserNameIncludeDeleted(userName) > 0) {
            log.warn("商户 {} 的登录名 {} 已被历史账号占用，跳过开户", merchant.getId(), userName);
            return;
        }
        SysRole role = merchantRole(merchant.getTenantId());
        if (role == null) {
            log.warn("运营中心 {} 缺少启用的 MERCHANT 角色，商户 {} 跳过开户",
                    merchant.getTenantId(), merchant.getId());
            return;
        }
        SysUser user = new SysUser();
        user.setTenantId(merchant.getTenantId());
        user.setUserType(USER_TYPE_MERCHANT);
        user.setUserName(userName);
        user.setPassword(passwordEncoder.encode(DEFAULT_ACCOUNT_PASSWORD));
        user.setRealName(realName);
        user.setPhone(phone);
        user.setMerchantId(merchant.getId());
        user.setStatus(isEnabled(merchant) ? STATUS_ENABLED : STATUS_DISABLED);
        scoped(() -> userMapper.insert(user));
        SysUserRole relation = new SysUserRole();
        relation.setUserId(user.getId());
        relation.setRoleId(role.getId());
        userRoleMapper.insert(relation);
        attachMerchant(user.getId(), merchant, 1);
        log.info("商户 {} 已开通{}小程序账号，登录手机号 {}", merchant.getId(), userLabel, phone);
    }

    /**
     * 同一手机号对应的启用商户账号；多店场景直接复用该账号。
     */
    private SysUser accountByPhone(String phone) {
        return userMapper.selectByPhone(phone).stream()
                .filter(user -> USER_TYPE_MERCHANT.equals(user.getUserType()))
                .filter(user -> user.getStatus() != null && user.getStatus() == STATUS_ENABLED)
                .findFirst().orElse(null);
    }

    private void attachMerchant(Long userId, Merchant merchant) {
        attachMerchant(userId, merchant, 0);
    }

    private void attachMerchant(Long userId, Merchant merchant, Integer isDefault) {
        Long exists = userMerchantMapper.selectCount(new LambdaQueryWrapper<SysUserMerchant>()
                .eq(SysUserMerchant::getUserId, userId)
                .eq(SysUserMerchant::getMerchantId, merchant.getId()));
        if (exists != null && exists > 0) {
            return;
        }
        SysUserMerchant relation = new SysUserMerchant();
        relation.setTenantId(merchant.getTenantId());
        relation.setUserId(userId);
        relation.setMerchantId(merchant.getId());
        relation.setIsDefault(isDefault);
        userMerchantMapper.insert(relation);
    }

    private void syncAccount(SysUser account, Merchant merchant, String phone, String realName) {
        String targetPhone = phone;
        if (!phone.equals(account.getPhone()) && phoneTakenByOther(phone, account.getId())) {
            log.warn("商户 {} 的新手机号 {} 已被其他账号占用，保留原登录手机号", merchant.getId(), phone);
            targetPhone = account.getPhone();
        }
        final String newPhone = targetPhone;
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getId, account.getId())
                .set(SysUser::getRealName, realName)
                .set(SysUser::getUpdateTime, LocalDateTime.now());
        if (StringUtils.hasText(newPhone)) {
            wrapper.set(SysUser::getPhone, newPhone);
        }
        Integer targetStatus = accountStatus(merchant);
        if (targetStatus != null) {
            wrapper.set(SysUser::getStatus, targetStatus);
        }
        scopedVoid(() -> userMapper.update(null, wrapper));
    }

    private void syncMerchantAccountStatus(Long merchantId, Integer status) {
        List<SysUser> accounts = scoped(() -> userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getMerchantId, merchantId)
                .eq(SysUser::getUserType, USER_TYPE_MERCHANT)));
        for (SysUser account : accounts) {
            scopedVoid(() -> userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                    .eq(SysUser::getId, account.getId())
                    .set(SysUser::getStatus, status)
                    .set(SysUser::getUpdateTime, LocalDateTime.now())));
        }
    }

    private SysRole merchantRole(Long tenantId) {
        return scoped(() -> roleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getTenantId, tenantId)
                .eq(SysRole::getRoleKey, MERCHANT_ROLE_KEY)
                .eq(SysRole::getStatus, STATUS_ENABLED)
                .last("LIMIT 1")));
    }

    private boolean phoneTakenByOther(String phone, Long selfUserId) {
        return userMapper.selectByPhoneIncludeDeleted(phone).stream()
                .anyMatch(user -> !Objects.equals(user.getId(), selfUserId));
    }

    private String contactName(Merchant merchant) {
        return StringUtils.hasText(merchant.getContact1()) ? merchant.getContact1() : merchant.getName();
    }

    private String ownerName(Merchant merchant) {
        return StringUtils.hasText(merchant.getOwnerName()) ? merchant.getOwnerName() : merchant.getName();
    }

    private boolean isEnabled(Merchant merchant) {
        return merchant.getStatus() != null && merchant.getStatus() == STATUS_ENABLED;
    }

    private Integer accountStatus(Merchant merchant) {
        Integer status = merchant.getStatus();
        if (status != null && (status == STATUS_ENABLED || status == STATUS_DISABLED)) {
            return status;
        }
        return null;
    }

    private Merchant requireMerchant(Long id) {
        Merchant merchant = id == null ? null : getById(id);
        if (merchant == null) {
            throw new BizException("商户不存在或无权操作");
        }
        return merchant;
    }

    private void validate(MerchantSaveDTO request) {
        if (!StringUtils.hasText(request.getName())) {
            throw new BizException("商户名称不能为空");
        }
        if (request.getName().length() > NAME_MAX_LENGTH) {
            throw new BizException("商户名称不能超过 64 字");
        }
        if (!StringUtils.hasText(request.getType())) {
            throw new BizException("商户类型不能为空");
        }
        Long typeCount = merchantTypeMapper.selectCount(new LambdaQueryWrapper<DictMerchantType>()
                .eq(DictMerchantType::getCode, request.getType())
                .eq(DictMerchantType::getStatus, STATUS_ENABLED));
        if (typeCount == null || typeCount == 0) {
            throw new BizException("商户类型不存在或已禁用");
        }
        if (request.getMerchantKind() == null
                || (request.getMerchantKind() != KIND_NINE_SMALL
                && request.getMerchantKind() != KIND_SOCIAL_UNIT)) {
            throw new BizException("场所性质不合法");
        }
        if (!StringUtils.hasText(request.getPhone())
                || !PHONE_PATTERN.matcher(request.getPhone()).matches()) {
            throw new BizException("联系电话格式不正确");
        }
        if (!StringUtils.hasText(request.getAddr())) {
            throw new BizException("地址不能为空");
        }
        if (request.getLng() == null || request.getLat() == null) {
            throw new BizException("请在地图上选择商户位置");
        }
        if (request.getLng() < -180 || request.getLng() > 180
                || request.getLat() < -90 || request.getLat() > 90) {
            throw new BizException("经纬度超出合法范围");
        }
        if (request.getEmployeeNum() != null
                && (request.getEmployeeNum() < 0 || request.getEmployeeNum() > EMPLOYEE_MAX)) {
            throw new BizException("员工人数超出合法范围");
        }
        if (request.getStatus() != null
                && request.getStatus() != STATUS_DISABLED && request.getStatus() != STATUS_ENABLED) {
            throw new BizException("商户状态不合法");
        }
    }

    private Merchant toEntity(MerchantSaveDTO request) {
        Merchant merchant = new Merchant();
        merchant.setName(request.getName());
        merchant.setType(request.getType());
        merchant.setMerchantKind(request.getMerchantKind());
        merchant.setLng(request.getLng());
        merchant.setLat(request.getLat());
        merchant.setAddr(request.getAddr());
        merchant.setArea(request.getArea());
        merchant.setOwnerName(request.getOwnerName());
        merchant.setPhone(request.getPhone());
        merchant.setBusinessScope(request.getBusinessScope());
        merchant.setBusinessContent(request.getBusinessContent());
        merchant.setFloorage(request.getFloorage());
        merchant.setEmployeeNum(request.getEmployeeNum());
        merchant.setAllowNum(request.getAllowNum());
        merchant.setBusinessHours(request.getBusinessHours());
        merchant.setRoundEnv(request.getRoundEnv());
        merchant.setPlaceFlag(request.getPlaceFlag());
        merchant.setContact1(request.getContact1());
        merchant.setPhone1(request.getPhone1());
        merchant.setContact2(request.getContact2());
        merchant.setPhone2(request.getPhone2());
        merchant.setBusinessLicense(request.getBusinessLicense());
        merchant.setFireRemark(request.getFireRemark());
        merchant.setPlacePic1(request.getPlacePic1());
        merchant.setPlacePic2(request.getPlacePic2());
        merchant.setPlacePic3(request.getPlacePic3());
        merchant.setRemark(request.getRemark());
        merchant.setInsureFlag(request.getInsureFlag());
        merchant.setStatus(request.getStatus());
        return merchant;
    }

    private void enrich(List<MerchantVO> records) {
        if (records.isEmpty()) {
            return;
        }
        List<Long> merchantIds = records.stream().map(MerchantVO::getId).collect(Collectors.toList());
        Map<String, String> typeNames = merchantTypeNames(records);
        Map<Long, List<String>> workAreaNames = workAreaNames(merchantIds);
        Map<Long, Long> deviceCounts = deviceCounts(merchantIds);
        Map<Long, String> loginAccounts = loginAccounts(merchantIds);
        for (MerchantVO record : records) {
            record.setTypeName(typeNames.get(record.getType()));
            record.setWorkAreaNames(workAreaNames.getOrDefault(record.getId(), Collections.emptyList()));
            record.setDeviceCount(deviceCounts.getOrDefault(record.getId(), 0L));
            record.setLoginAccount(loginAccounts.get(record.getId()));
        }
    }

    private Map<String, String> merchantTypeNames(List<MerchantVO> records) {
        Set<String> codes = records.stream().map(MerchantVO::getType)
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }
        return merchantTypeMapper.selectList(new LambdaQueryWrapper<DictMerchantType>()
                        .in(DictMerchantType::getCode, codes))
                .stream().collect(Collectors.toMap(DictMerchantType::getCode, DictMerchantType::getName,
                        (a, b) -> a));
    }

    private Map<Long, List<String>> workAreaNames(List<Long> merchantIds) {
        List<SysWorkAreaMerchant> relations = scoped(() -> workAreaMerchantMapper.selectList(
                new LambdaQueryWrapper<SysWorkAreaMerchant>()
                        .in(SysWorkAreaMerchant::getMerchantId, merchantIds)));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> areaIds = relations.stream().map(SysWorkAreaMerchant::getWorkAreaId)
                .distinct().collect(Collectors.toList());
        Map<Long, String> names = scoped(() -> workAreaMapper.selectBatchIds(areaIds)).stream()
                .collect(Collectors.toMap(SysWorkArea::getId, SysWorkArea::getName, (a, b) -> a));
        Map<Long, List<String>> result = new HashMap<>();
        for (SysWorkAreaMerchant relation : relations) {
            String name = names.get(relation.getWorkAreaId());
            if (name == null) {
                continue;
            }
            result.computeIfAbsent(relation.getMerchantId(), key -> new ArrayList<>()).add(name);
        }
        return result;
    }

    private Map<Long, Long> deviceCounts(List<Long> merchantIds) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.select("merchant_id", "count(*) as cnt")
                .in("merchant_id", merchantIds)
                .groupBy("merchant_id");
        List<Map<String, Object>> rows = scoped(() -> deviceMapper.selectMaps(wrapper));
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object merchantId = row.get("merchant_id");
            Object count = row.get("cnt");
            if (merchantId instanceof Number && count instanceof Number) {
                result.put(((Number) merchantId).longValue(), ((Number) count).longValue());
            }
        }
        return result;
    }

    private Map<Long, String> loginAccounts(List<Long> merchantIds) {
        List<SysUserMerchant> relations = scoped(() -> userMerchantMapper.selectList(
                new LambdaQueryWrapper<SysUserMerchant>().in(SysUserMerchant::getMerchantId, merchantIds)));
        if (relations.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = relations.stream().map(SysUserMerchant::getUserId)
                .distinct().collect(Collectors.toList());
        Map<Long, String> userNames = scoped(() -> userMapper.selectBatchIds(userIds)).stream()
                .filter(user -> USER_TYPE_MERCHANT.equals(user.getUserType()))
                .filter(user -> user.getStatus() != null && user.getStatus() == STATUS_ENABLED)
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUserName, (a, b) -> a));
        Map<Long, String> result = new HashMap<>();
        for (SysUserMerchant relation : relations) {
            String userName = userNames.get(relation.getUserId());
            if (userName != null) {
                result.putIfAbsent(relation.getMerchantId(), userName);
            }
        }
        return result;
    }

    private List<WorkAreaBriefVO> workAreasOfMerchant(Long merchantId) {
        List<SysWorkAreaMerchant> relations = scoped(() -> workAreaMerchantMapper.selectList(
                new LambdaQueryWrapper<SysWorkAreaMerchant>()
                        .eq(SysWorkAreaMerchant::getMerchantId, merchantId)));
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> areaIds = relations.stream().map(SysWorkAreaMerchant::getWorkAreaId)
                .distinct().collect(Collectors.toList());
        return scoped(() -> workAreaMapper.selectBatchIds(areaIds)).stream()
                .map(MerchantConverter::toWorkAreaBriefVO).collect(Collectors.toList());
    }

    private List<BuildingNodeVO> buildingsOfMerchant(Merchant merchant) {
        List<MerchantBuilding> buildings = scoped(() -> buildingMapper.selectList(
                new LambdaQueryWrapper<MerchantBuilding>()
                        .eq(MerchantBuilding::getMerchantId, merchant.getId())
                        .orderByAsc(MerchantBuilding::getSort)
                        .orderByAsc(MerchantBuilding::getId)));
        if (buildings.isEmpty()) {
            return Collections.emptyList();
        }
        List<MerchantFloor> floors = scoped(() -> floorMapper.selectList(
                new LambdaQueryWrapper<MerchantFloor>()
                        .eq(MerchantFloor::getMerchantId, merchant.getId())
                        .orderByAsc(MerchantFloor::getFloorNo)
                        .orderByAsc(MerchantFloor::getId)));
        Map<Long, Long> floorDeviceCounts = floorDeviceCounts(merchant.getId());
        Map<Long, List<FloorNodeVO>> floorsByBuilding = new HashMap<>();
        for (MerchantFloor floor : floors) {
            FloorNodeVO node = FloorConverter.toNodeVO(floor);
            node.setDeviceCount(floorDeviceCounts.getOrDefault(floor.getId(), 0L));
            floorsByBuilding.computeIfAbsent(floor.getBuildingId(), key -> new ArrayList<>()).add(node);
        }
        List<BuildingNodeVO> result = new ArrayList<>(buildings.size());
        for (MerchantBuilding building : buildings) {
            BuildingNodeVO node = BuildingConverter.toNodeVO(building);
            node.setFloors(floorsByBuilding.getOrDefault(building.getId(), Collections.emptyList()));
            result.add(node);
        }
        return result;
    }

    private Map<Long, Long> floorDeviceCounts(Long merchantId) {
        QueryWrapper<Device> wrapper = new QueryWrapper<>();
        wrapper.select("floor_id", "count(*) as cnt")
                .eq("merchant_id", merchantId)
                .isNotNull("floor_id")
                .groupBy("floor_id");
        List<Map<String, Object>> rows = scoped(() -> deviceMapper.selectMaps(wrapper));
        Map<Long, Long> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object floorId = row.get("floor_id");
            Object count = row.get("cnt");
            if (floorId instanceof Number && count instanceof Number) {
                result.put(((Number) floorId).longValue(), ((Number) count).longValue());
            }
        }
        return result;
    }

    private List<MerchantDeviceBriefVO> devicesOfMerchant(Merchant merchant) {
        List<Device> devices = scoped(() -> deviceMapper.selectList(new LambdaQueryWrapper<Device>()
                .eq(Device::getMerchantId, merchant.getId())
                .orderByDesc(Device::getCreateTime)));
        if (devices.isEmpty()) {
            return Collections.emptyList();
        }
        Map<String, String> typeNames = deviceTypeNames(devices);
        Map<Long, String> buildingNames = buildingNames(devices);
        Map<Long, String> floorNames = floorNames(devices);
        return devices.stream().map(device -> DeviceConverter.toMerchantBriefVO(device,
                typeNames.get(device.getType()),
                locationText(device, buildingNames, floorNames))).collect(Collectors.toList());
    }

    private Map<String, String> deviceTypeNames(List<Device> devices) {
        Set<String> codes = devices.stream().map(Device::getType)
                .filter(StringUtils::hasText).collect(Collectors.toSet());
        if (codes.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceTypeMapper.selectList(new LambdaQueryWrapper<DictDeviceType>()
                        .in(DictDeviceType::getCode, codes))
                .stream().collect(Collectors.toMap(DictDeviceType::getCode, DictDeviceType::getName,
                        (a, b) -> a));
    }

    private Map<Long, String> buildingNames(List<Device> devices) {
        List<Long> ids = devices.stream().map(Device::getBuildingId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return scoped(() -> buildingMapper.selectBatchIds(ids)).stream()
                .collect(Collectors.toMap(MerchantBuilding::getId, MerchantBuilding::getName, (a, b) -> a));
    }

    private Map<Long, String> floorNames(List<Device> devices) {
        List<Long> ids = devices.stream().map(Device::getFloorId)
                .filter(id -> id != null).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return scoped(() -> floorMapper.selectBatchIds(ids)).stream()
                .collect(Collectors.toMap(MerchantFloor::getId, MerchantFloor::getName, (a, b) -> a));
    }

    private String locationText(Device device, Map<Long, String> buildingNames, Map<Long, String> floorNames) {
        Integer locationType = device.getLocationType();
        if (locationType != null && locationType == Device.LOCATION_GPS) {
            return "GPS " + device.getLng() + ", " + device.getLat();
        }
        if (locationType != null && locationType == Device.LOCATION_FLOOR) {
            String building = buildingNames.get(device.getBuildingId());
            String floor = floorNames.get(device.getFloorId());
            String base = (building == null ? "楼宇" : building) + " / " + (floor == null ? "楼层" : floor);
            if (device.getFloorX() == null || device.getFloorY() == null) {
                return base;
            }
            return base + " (" + device.getFloorX().stripTrailingZeros().toPlainString() + "%, "
                    + device.getFloorY().stripTrailingZeros().toPlainString() + "%)";
        }
        return "未设置";
    }

    private List<Long> merchantIdsOfWorkArea(Long workAreaId) {
        return scoped(() -> workAreaMerchantMapper.selectList(new LambdaQueryWrapper<SysWorkAreaMerchant>()
                        .eq(SysWorkAreaMerchant::getWorkAreaId, workAreaId)))
                .stream().map(SysWorkAreaMerchant::getMerchantId).distinct().collect(Collectors.toList());
    }

    private List<Long> distinctIds(List<Long> ids) {
        if (ids == null) {
            return Collections.emptyList();
        }
        return ids.stream().filter(id -> id != null).distinct().collect(Collectors.toList());
    }

    private void validateWorkAreas(Long tenantId, List<Long> workAreaIds) {
        if (workAreaIds.isEmpty()) {
            return;
        }
        List<SysWorkArea> areas = scoped(() -> workAreaMapper.selectBatchIds(workAreaIds));
        if (areas.size() != workAreaIds.size()) {
            throw new BizException("部分工作区不存在或无权操作");
        }
        boolean mismatch = areas.stream().anyMatch(area -> !tenantId.equals(area.getTenantId()));
        if (mismatch) {
            throw new BizException("工作区与商户不属于同一运营中心");
        }
    }

    private <T> T scoped(Supplier<T> action) {
        if (!dataScopeService.isPlatform()) {
            return action.get();
        }
        return dataScopeService.withIgnoreTenant(action);
    }

    private void scopedVoid(Runnable action) {
        if (!dataScopeService.isPlatform()) {
            action.run();
            return;
        }
        dataScopeService.withIgnoreTenant(() -> {
            action.run();
            return null;
        });
    }
}
