import { Reducer } from 'redux';

import {
  FeatureAction,
  IArtifactManagerState,
  loadArtifactUrlActionTypes,
  loadArtifactPreviewActionTypes,
  resetActionType,
  loadDatasetVersionActionTypes,
} from '../types';

const initial: IArtifactManagerState['data'] = {
  url: null,
  preview: null,
  datasetVersions: {},
};

const dataReducer: Reducer<IArtifactManagerState['data'], FeatureAction> = (
  state = initial,
  action
) => {
  switch (action.type) {
    case loadArtifactUrlActionTypes.REQUEST: {
      return {
        ...state,
        errorMessage: null,
      };
    }
    case loadArtifactUrlActionTypes.SUCCESS: {
      return {
        ...state,
        url: action.payload.url,
      };
    }
    case loadArtifactPreviewActionTypes.SUCCESS: {
      return { ...state, preview: action.payload.preview };
    }
    case loadArtifactUrlActionTypes.FAILURE: {
      return {
        ...state,
        errorMessage: action.payload,
      };
    }
    case loadDatasetVersionActionTypes.SUCCESS: {
      return {
        ...state,
        datasetVersions: {
          ...state.datasetVersions,
          [action.payload.datasetVersionId]: action.payload.datasetVersion,
        },
      };
    }
    case resetActionType.RESET: {
      return initial;
    }
    default:
      return state;
  }
};

export default dataReducer;
