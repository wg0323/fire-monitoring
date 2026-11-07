import type { MenuNode } from '@/types/api';

export function flattenMenus(menus: MenuNode[]): MenuNode[] {
  const result: MenuNode[] = [];
  const walk = (nodes: MenuNode[]): void => {
    for (const node of nodes) {
      result.push(node);
      if (node.children?.length) {
        walk(node.children);
      }
    }
  };
  walk(menus);
  return result;
}

export function collectPaths(menus: MenuNode[]): string[] {
  return flattenMenus(menus)
    .filter((item) => item.type === 1 && item.path)
    .map((item) => item.path as string);
}

export function findMenuTrail(menus: MenuNode[], path: string): MenuNode[] {
  const trail: MenuNode[] = [];
  const walk = (nodes: MenuNode[], parents: MenuNode[]): boolean => {
    for (const node of nodes) {
      const nextParents = [...parents, node];
      if (node.path === path) {
        trail.push(...nextParents);
        return true;
      }
      if (node.children?.length && walk(node.children, nextParents)) {
        return true;
      }
    }
    return false;
  };
  walk(menus, []);
  return trail;
}

export function menuTitleByPath(menus: MenuNode[], path: string): string {
  const trail = findMenuTrail(menus, path);
  return trail.length ? trail[trail.length - 1].name : '';
}

export function firstMenuPath(menus: MenuNode[]): string {
  return collectPaths(menus)[0] ?? '';
}

export function hasMenuPath(menus: MenuNode[], path: string): boolean {
  return collectPaths(menus).includes(path);
}
