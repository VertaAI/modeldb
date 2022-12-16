import * as React from 'react';
import { useDispatch, useSelector } from 'react-redux';

import { IArtifactWithPath } from 'shared/models/Artifact';
import { initialCommunication } from 'shared/utils/redux/communication';

import { selectDownloadingArtifact } from '../selectors';
import { reset, downloadArtifact } from '../actions';
import { EntityType, getDownloadArtifactsKey } from '../types';

interface ILocalProps {
  entityId: string;
  entityType: EntityType;
  artifact: IArtifactWithPath;
}

const useDownloadArtifact = (props: ILocalProps) => {
  const dispatch = useDispatch();
  const downloadingArtifact = useSelector(selectDownloadingArtifact);

  React.useEffect(() => {
    return () => {
      dispatch(reset());
    };
  }, []);

  return {
    downloadingArtifact:
      downloadingArtifact[
        getDownloadArtifactsKey({
          entityId: props.entityId,
          key: props.artifact.key,
        })
      ] || initialCommunication,
    downloadArtifact: () => {
      dispatch(
        downloadArtifact(props.entityType, props.entityId, props.artifact)
      );
    },
    reset: () => dispatch(reset()),
  };
};

export default useDownloadArtifact;
