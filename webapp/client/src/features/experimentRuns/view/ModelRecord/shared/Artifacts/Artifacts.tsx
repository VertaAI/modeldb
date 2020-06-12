import * as React from 'react';

import ModelRecord from 'models/ModelRecord';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';

import Artifact from './Artifact';
import styles from './Artifacts.module.css';

interface ILocalProps {
  modelRecordId: ModelRecord['id'];
  artifacts: ModelRecord['artifacts'];
  allowedActions: ModelRecord['allowedActions'];
}

const Artifacts = ({
  artifacts,
  modelRecordId,
  allowedActions,
}: ILocalProps) => {
  return (
    <div className={styles.root}>
      <ScrollableContainer
        maxHeight={180}
        containerOffsetValue={12}
        children={
          <div className={styles.artifacts}>
            {artifacts.map(artifact => (
              <Artifact
                key={artifact.path}
                artifact={artifact}
                entityId={modelRecordId}
                entityType="experimentRun"
                allowedActions={allowedActions}
              />
            ))}
          </div>
        }
      />
    </div>
  );
};

export default Artifacts;
