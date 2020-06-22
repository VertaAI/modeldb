import { action } from 'typesafe-actions';

import { EntityWithDescription } from 'shared/models/Description';
import normalizeError from 'shared/utils/normalizeError';
import { updateDatasetDesc } from 'features/datasets/store';
import { updateDatasetVersionDesc } from 'features/datasetVersions/store';
import { updateExpRunDesc } from 'features/experimentRuns/store';
import { updateExperimentDescription } from 'features/experiments/store';
import { updateProjectDesc } from 'features/projects/store';
import { ActionResult } from 'setup/store/store';

import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';
import { addOrEditDescActionTypes } from './types';

export const addOrEditDescription = (
  id: string,
  description: string,
  entityType: EntityWithDescription
): ActionResult<void> => async (dispatch, getState, { ServiceFactory }) => {
  dispatch(action(addOrEditDescActionTypes.REQUEST, { id }));

  await ServiceFactory.crudDescService()
    .addOrEditDescription(id, description, entityType)
    .then(res => {
      dispatch(
        action(addOrEditDescActionTypes.SUCCESS, {
          id,
          data: { description: res },
        })
      );
      switch (entityType) {
        case 'experimentRun': {
          dispatch(updateExpRunDesc(id, res));
          return;
        }
        case 'project': {
          dispatch(updateProjectDesc(id, res));
          return;
        }
        case 'experiment': {
          dispatch(updateExperimentDescription(id, res));
          return;
        }
        case 'dataset': {
          dispatch(updateDatasetDesc(id, res));
          return;
        }
        case 'datasetVersion': {
          dispatch(updateDatasetVersionDesc(id, res));
          return;
        }
        default:
          return exhaustiveCheck(entityType, '');
      }
    })
    .catch(error => {
      dispatch(
        action(addOrEditDescActionTypes.FAILURE, {
          id,
          error: normalizeError(error),
        })
      );
    });
};
