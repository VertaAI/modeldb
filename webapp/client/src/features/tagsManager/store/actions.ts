import { action } from 'typesafe-actions';

import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import normalizeError from 'shared/utils/normalizeError';
import { ThunkDispatch } from 'redux-thunk';
import { updateDatasetTags } from 'features/datasets/store';
import { updateDatasetVersionTags } from 'features/datasetVersions/store';
import { updateExpRunTags } from 'features/experimentRuns/store';
import { updateExperimentTags } from 'features/experiments/store';
import { updateProjectTags } from 'features/projects/store';
import { ActionResult } from 'setup/store/store';

import { addTagActionTypes, removeTagActionTypes } from './types';
import { EntityWithTags } from 'shared/models/TagsCRUD';

export const removeTag = (
  id: string,
  tags: string[], // the backend requires a list but the frontend only support deletion/addition on a single tag at the moment
  entityType: EntityWithTags,
  isDeleteAll?: boolean
): ActionResult<void> => async (dispatch, getState, { ServiceFactory }) => {
  dispatch(action(removeTagActionTypes.REQUEST, { id, tags }));

  await ServiceFactory.crudTagsService()
    .removeTag(id, tags, entityType, isDeleteAll)
    .then(tagsResponse => {
      dispatch(
        action(removeTagActionTypes.SUCCESS, { id, tags: tagsResponse })
      );
      updateEntityTag(dispatch, entityType, id, tagsResponse);
    })
    .catch(error => {
      dispatch(
        action(removeTagActionTypes.FAILURE, {
          id,
          error: normalizeError(error),
        })
      );
    });
};

export const addTag = (
  id: string,
  tags: string[], // the backend requires a list but the frontend only support deletion/addition on a single tag at the moment
  entityType: EntityWithTags
): ActionResult<void> => async (dispatch, getState, { ServiceFactory }) => {
  dispatch(action(addTagActionTypes.REQUEST, { id, tags }));
  await ServiceFactory.crudTagsService()
    .addTag(id, tags, entityType)
    .then(tagsResponse => {
      // todo we have cycle dependencies. Need fix it
      if (tagsResponse) {
        dispatch(action(addTagActionTypes.SUCCESS, { id, tags: tagsResponse }));
        updateEntityTag(dispatch, entityType, id, tagsResponse);
      }
    })
    .catch(error => {
      dispatch(
        action(addTagActionTypes.FAILURE, { id, error: normalizeError(error) })
      );
    });
};

const updateEntityTag = (
  dispatch: ThunkDispatch<any, any, any>,
  entityType: EntityWithTags,
  id: string,
  newTags: string[]
) => {
  switch (entityType) {
    case 'experimentRun': {
      dispatch(updateExpRunTags(id, newTags));
      return;
    }
    case 'dataset': {
      dispatch(updateDatasetTags(id, newTags));
      return;
    }
    case 'datasetVersion': {
      dispatch(updateDatasetVersionTags(id, newTags));
      return;
    }
    case 'experiment': {
      dispatch(updateExperimentTags(id, newTags));
      return;
    }
    case 'project': {
      dispatch(updateProjectTags(id, newTags));
      return;
    }
    default:
      return exhaustiveCheck(entityType, '');
  }
};
