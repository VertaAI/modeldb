export type AllEntityAction = UserEntityAction | 'ALL' | 'READ' | 'PUBLIC_READ';

export type UserEntityAction = 'create' | 'update' | 'delete' | 'deploy';

export interface IEntityWithAllowedActions {
  allowedActions: AllEntityAction[];
}

export const hasAccessToAction = (
  action: any,
  entity: any
): boolean => {
  return true;
};
