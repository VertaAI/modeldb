import * as React from 'react';

import { IArtifactWithDatasetVersion } from 'core/shared/models/Artifact';
import { IDatasetVersion } from 'models/DatasetVersion';
import {
  selectCommunications,
  selectDatasetVersion,
  loadDatasetVersion,
} from 'store/artifactManager';
import {
  initialCommunication,
  ICommunication,
} from 'core/shared/utils/redux/communication';
import { useSelector, useDispatch } from 'react-redux';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

export const useArtifactDatasetVersion = ({
  artifact,
}: {
  artifact: IArtifactWithDatasetVersion;
}) => {
  const loadingDatasetVersion = useSelector((state: IApplicationState) =>
    artifact.linkedArtifactId
      ? selectCommunications(state).loadingDatasetVersions[
          artifact.linkedArtifactId
        ] || initialCommunication
      : initialCommunication
  );
  const datasetVersion = useSelector((state: IApplicationState) =>
    artifact.linkedArtifactId
      ? selectDatasetVersion(state, artifact.linkedArtifactId) || null
      : null
  );
  const dispatch = useDispatch();
  const workspaceName = useSelector(selectCurrentWorkspaceName);
  React.useEffect(() => {
    if (artifact.linkedArtifactId) {
      dispatch(loadDatasetVersion(workspaceName, artifact.linkedArtifactId));
    }
  }, [artifact.linkedArtifactId]);

  return { datasetVersion, loadingDatasetVersion };
};

// for legacy code
export const ArtifactDatasetVersion = ({
  artifact,
  children,
}: {
  artifact: IArtifactWithDatasetVersion;
  children: (data: ReturnType<typeof useArtifactDatasetVersion>) => JSX.Element;
}) => {
  const data = useArtifactDatasetVersion({ artifact });
  return children(data);
};
