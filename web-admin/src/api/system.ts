import { http } from '@/utils/request';
import type {
  DeptNode,
  DictItem,
  MerchantOption,
  MenuNode,
  OrgItem,
  PageResult,
  RoleItem,
  UserItem,
  WorkAreaItem,
} from '@/types/api';

export interface PageParams {
  pageNum: number;
  pageSize: number;
}

export interface UserQuery extends PageParams {
  keyword?: string;
  userType?: string;
  tenantId?: string;
  status?: number;
}

export interface RoleQuery extends PageParams {
  keyword?: string;
  status?: number;
}

export interface OrgQuery extends PageParams {
  keyword?: string;
  status?: number;
}

export interface WorkAreaQuery extends PageParams {
  keyword?: string;
  tenantId?: string;
}

export interface UserSavePayload {
  tenantId?: string;
  deptId?: string | null;
  userType: string;
  userName: string;
  password?: string;
  realName: string;
  phone: string;
  merchantId?: string | null;
  roleId?: string;
  status: number;
  workAreaIds?: string[];
}

export interface RoleSavePayload {
  roleName: string;
  roleKey: string;
  status: number;
  menuIds?: string[];
}

export interface MenuSavePayload {
  parentId: string;
  name: string;
  menuKey: string;
  type: number;
  path?: string | null;
  perms?: string | null;
  seq: number;
  status: number;
  platformOnly: number;
}

export interface OrgSavePayload {
  name: string;
  code?: string;
  adcode?: string;
  address?: string;
  status: number;
}

export interface WorkAreaSavePayload {
  tenantId?: string;
  name: string;
  remark?: string;
  userIds?: string[];
  merchantIds?: string[];
}

export function pageUsers(params: UserQuery): Promise<PageResult<UserItem>> {
  return http.get<PageResult<UserItem>>('/system/user/page', params);
}

export function getUser(id: string): Promise<UserItem> {
  return http.get<UserItem>(`/system/user/${id}`);
}

export function createUser(payload: UserSavePayload): Promise<string> {
  return http.post<string>('/system/user', payload);
}

export function updateUser(id: string, payload: UserSavePayload): Promise<void> {
  return http.put<void>(`/system/user/${id}`, payload);
}

export function deleteUser(id: string): Promise<void> {
  return http.delete<void>(`/system/user/${id}`);
}

export function bindUserWorkAreas(id: string, ids: string[]): Promise<void> {
  return http.put<void>(`/system/user/${id}/work-areas`, { ids });
}

export function resetUserPassword(id: string, password?: string): Promise<void> {
  return http.put<void>(`/system/user/${id}/password`, { password });
}

export function userOptions(tenantId?: string, userType?: string): Promise<UserItem[]> {
  return http.get<UserItem[]>('/system/user/options', { tenantId, userType });
}

export function merchantOptions(tenantId?: string): Promise<MerchantOption[]> {
  return http.get<MerchantOption[]>('/device/merchant/options', { tenantId });
}

export function pageRoles(params: RoleQuery): Promise<PageResult<RoleItem>> {
  return http.get<PageResult<RoleItem>>('/system/role/page', params);
}

export function listRoles(tenantId?: string, userType?: string): Promise<RoleItem[]> {
  return http.get<RoleItem[]>('/system/role/list', { tenantId, userType });
}

export function createRole(payload: RoleSavePayload): Promise<string> {
  return http.post<string>('/system/role', payload);
}

export function updateRole(id: string, payload: RoleSavePayload): Promise<void> {
  return http.put<void>(`/system/role/${id}`, payload);
}

export function deleteRole(id: string): Promise<void> {
  return http.delete<void>(`/system/role/${id}`);
}

export function roleMenuIds(id: string): Promise<string[]> {
  return http.get<string[]>(`/system/role/${id}/menus`);
}

export function assignRoleMenus(id: string, ids: string[]): Promise<void> {
  return http.put<void>(`/system/role/${id}/menus`, { ids });
}

export function menuTree(): Promise<MenuNode[]> {
  return http.get<MenuNode[]>('/system/menu/tree');
}

export function createMenu(payload: MenuSavePayload): Promise<string> {
  return http.post<string>('/system/menu', payload);
}

export function updateMenu(id: string, payload: MenuSavePayload): Promise<void> {
  return http.put<void>(`/system/menu/${id}`, payload);
}

export function deleteMenu(id: string): Promise<void> {
  return http.delete<void>(`/system/menu/${id}`);
}

export function pageTenants(params: OrgQuery): Promise<PageResult<OrgItem>> {
  return http.get<PageResult<OrgItem>>('/system/tenant/page', params);
}

export function listTenants(): Promise<OrgItem[]> {
  return http.get<OrgItem[]>('/system/tenant/list');
}

export function getTenant(id: string): Promise<OrgItem> {
  return http.get<OrgItem>(`/system/tenant/${id}`);
}

export function createTenant(payload: OrgSavePayload): Promise<string> {
  return http.post<string>('/system/tenant', payload);
}

export function updateTenant(id: string, payload: OrgSavePayload): Promise<void> {
  return http.put<void>(`/system/tenant/${id}`, payload);
}

export function deleteTenant(id: string): Promise<void> {
  return http.delete<void>(`/system/tenant/${id}`);
}

export function pageWorkAreas(params: WorkAreaQuery): Promise<PageResult<WorkAreaItem>> {
  return http.get<PageResult<WorkAreaItem>>('/system/work-area/page', params);
}

export function workAreaOptions(tenantId?: string): Promise<WorkAreaItem[]> {
  return http.get<WorkAreaItem[]>('/system/work-area/options', { tenantId });
}

export function getWorkArea(id: string): Promise<WorkAreaItem> {
  return http.get<WorkAreaItem>(`/system/work-area/${id}`);
}

export function createWorkArea(payload: WorkAreaSavePayload): Promise<string> {
  return http.post<string>('/system/work-area', payload);
}

export function updateWorkArea(id: string, payload: WorkAreaSavePayload): Promise<void> {
  return http.put<void>(`/system/work-area/${id}`, payload);
}

export function deleteWorkArea(id: string): Promise<void> {
  return http.delete<void>(`/system/work-area/${id}`);
}

export function bindWorkAreaUsers(id: string, ids: string[]): Promise<void> {
  return http.put<void>(`/system/work-area/${id}/users`, { ids });
}

export function bindWorkAreaMerchants(id: string, ids: string[]): Promise<void> {
  return http.put<void>(`/system/work-area/${id}/merchants`, { ids });
}

export function deptTree(tenantId?: string): Promise<DeptNode[]> {
  return http.get<DeptNode[]>('/system/dept/tree', { tenantId });
}

export function listDict(type: string): Promise<DictItem[]> {
  return http.get<DictItem[]>(`/device/dict/manage/${type}`);
}

export interface DictSavePayload {
  code: string;
  name: string;
  remark?: string;
  status: number;
  sort?: number;
  offlineThreshold?: number | null;
  offlineUnit?: string | null;
  monitorFlag?: number | null;
}

export function createDict(type: string, payload: DictSavePayload): Promise<string> {
  return http.post<string>(`/device/dict/${type}`, payload);
}

export function updateDict(type: string, id: string, payload: DictSavePayload): Promise<string> {
  return http.put<string>(`/device/dict/${type}/${id}`, payload);
}

export function deleteDict(type: string, id: string): Promise<void> {
  return http.delete<void>(`/device/dict/${type}/${id}`);
}
