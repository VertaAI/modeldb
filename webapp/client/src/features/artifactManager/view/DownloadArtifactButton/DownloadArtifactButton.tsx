import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import { IArtifactWithPath } from 'core/shared/models/Artifact';
import { artifactErrorMessages } from 'core/shared/utils/customErrorMessages';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import Button from 'core/shared/view/elements/Button/Button';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { EntityType } from 'features/artifactManager/store';
import useDownloadArtifact from 'features/artifactManager/store/hooks/useDownloadArtifact';

import styles from './DownloadArtifactButton.module.css';

interface ILocalProps {
  entityId: string;
  entityType: EntityType;
  artifact: IArtifactWithPath;
  isShowErrorIfExist: boolean;
}

type AllProps = ILocalProps;

const DownloadArtifactButton = (props: AllProps) => {
  const { artifact, entityId, entityType, isShowErrorIfExist } = props;
  const { downloadArtifact, downloadingArtifact } = useDownloadArtifact({
    artifact,
    entityId,
    entityType,
  });

  if (R.equals(downloadingArtifact, initialCommunication)) {
    return (
      <div className={styles.root}>
        <Button onClick={downloadArtifact}>Download Artifact</Button>
      </div>
    );
  }
  if (downloadingArtifact.isRequesting) {
    return (
      <div className={styles.preloader}>
        <Preloader variant="dots" />
      </div>
    );
  }
  if (downloadingArtifact.error) {
    return (
      <div className={styles.root}>
        <Button disabled={true} onClick={downloadArtifact}>
          Download Failed
        </Button>
        {isShowErrorIfExist && (
          <div className={styles.url_call_message}>
            <InlineCommunicationError
              error={downloadingArtifact.error}
              customMessage={artifactErrorMessages.artifact_download}
            />
          </div>
        )}
      </div>
    );
  }
  return (
    <div className={styles.root}>
      <Button disabled={true}>Downloaded</Button>
      <div className={styles.url_call_message}>
        <div className={styles.success_message}>
          Downloaded <Icon type="check-circle" />
        </div>
      </div>
    </div>
  );
};

export default DownloadArtifactButton;
