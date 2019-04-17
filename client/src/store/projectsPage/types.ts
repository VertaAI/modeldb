export interface IProjectsPageState {
  data: {
    isShowDeveloperKeyInfo: boolean;
  };
}

export const hideDeveloperKeyInfoActionType =
  '@@projectsPage/HIDE_DEVELOPER_KEY_INFO';
export interface IHideDeveloperKeyInfo {
  type: typeof hideDeveloperKeyInfoActionType;
}

export type FeatureAction = IHideDeveloperKeyInfo;
