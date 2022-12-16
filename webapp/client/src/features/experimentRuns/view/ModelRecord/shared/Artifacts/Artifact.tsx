import * as React from 'react';
import { useSelector, useDispatch } from 'react-redux';

import { EntityType } from 'features/artifactManager/store';
import { IArtifact, checkArtifactWithPath } from 'shared/models/Artifact';
import useDownloadArtifact from 'features/artifactManager/store/hooks/useDownloadArtifact';
import {
  toastSuccess,
  toastError,
} from 'shared/view/elements/Notification/Notification';
import PileWithActions, {
  Action,
} from 'shared/view/elements/PileWithActions/PileWithActions';
import { IApplicationState } from 'setup/store/store';
import * as ExperimentRunsStore from 'features/experimentRuns/store';
import { hasAccessToAction } from 'shared/models/EntitiesActions';
import ModelRecord from 'shared/models/ModelRecord';
import { initialCommunication } from 'shared/utils/redux/communication';
import ConfirmAction from 'shared/view/elements/ConfirmAction/ConfirmAction';
import { useInfoAction } from './InfoAction/InfoAction';
import { artifactErrorMessages } from 'shared/utils/customErrorMessages';
import { communicationErrorToString } from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';

const Artifact = ({
  artifact,
  entityId,
  entityType,
  allowedActions,
}: {
  entityId: string;
  entityType: EntityType;
  artifact: IArtifact;
  allowedActions: ModelRecord['allowedActions'];
}) => {
  const iconType = (() => {
    if (artifact.key === 'query') {
      return 'query';
    }
    if (artifact.type === 'IMAGE') {
      return 'image';
    }
    if (artifact.type === 'BINARY') {
      return 'cube';
    }
    return 'codepen';
  })();

  const downloadArtifactAction = useDownloadArtifactAction({
    artifact,
    entityId,
    entityType,
  });
  const deleteArtifactAction = useDeleteArtifactAction({
    artifact,
    experimentRunId: entityId,
    allowedActions,
  });
  const infoAction = useInfoAction({
    artifact,
    entityId,
    entityType,
    popupIconType: iconType,
  });
  const actions = [infoAction, downloadArtifactAction, deleteArtifactAction];

  return (
    <PileWithActions
      pile={{
        iconType,
        label: artifact.key,
        title: artifact.key,
      }}
      isShowPreloader={actions.some(
        (action) => action && action.isShowPreloader
      )}
      actions={actions
        .map((action) => action && action.content)
        .filter((x): x is JSX.Element => Boolean(x))}
    />
  );
};

const useDownloadArtifactAction = ({
  artifact,
  entityId,
  entityType,
}: {
  entityId: string;
  entityType: EntityType;
  artifact: IArtifact;
}) => {
  if (!checkArtifactWithPath(artifact)) {
    return undefined;
  }
  const { downloadArtifact, downloadingArtifact, reset } = useDownloadArtifact({
    artifact,
    entityId,
    entityType,
  });

  React.useEffect(() => {
    if (downloadingArtifact.isSuccess) {
      toastSuccess(<span>The artifact is downloaded</span>);
      reset();
    }
  }, [downloadingArtifact.isSuccess]);
  React.useEffect(() => {
    if (downloadingArtifact.error) {
      toastError(
        `${
          artifactErrorMessages.artifact_download
        }: ${communicationErrorToString(downloadingArtifact.error)}`
      );
      reset();
    }
  }, [downloadingArtifact.error]);

  return {
    content: <Action iconType="download" onClick={downloadArtifact} />,
    isShowPreloader: downloadingArtifact.isRequesting,
  };
};

const useDeleteArtifactAction = ({
  artifact,
  experimentRunId,
  allowedActions,
}: {
  artifact: IArtifact;
  experimentRunId: string;
  allowedActions: ModelRecord['allowedActions'];
}) => {
  const deletingArtifacts = useSelector(
    (state: IApplicationState) =>
      ExperimentRunsStore.selectCommunications(state)
        .deletingExperimentRunArtifact
  );
  const dispatch = useDispatch();

  return hasAccessToAction('update', {
    allowedActions,
  })
    ? {
        isShowPreloader: (
          deletingArtifacts[`${experimentRunId}-${artifact.key}`] ||
          initialCommunication
        ).isRequesting,
        content: (
          <ConfirmAction
            title="Are you sure you want to delete this artifact?"
            confirmText="This action cannot be reversed."
            cancelButtonText="Cancel"
            confirmButtonText="Delete"
          >
            {(withConfirmAction) => (
              <Action
                iconType="delete"
                onClick={withConfirmAction(() =>
                  dispatch(
                    ExperimentRunsStore.deleteExperimentRunArtifact(
                      experimentRunId,
                      artifact.key
                    )
                  )
                )}
              />
            )}
          </ConfirmAction>
        ),
      }
    : undefined;
};

export default Artifact;
