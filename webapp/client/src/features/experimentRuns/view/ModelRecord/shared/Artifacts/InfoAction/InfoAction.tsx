import * as React from 'react';
import { Link } from 'react-router-dom';

import { Action } from 'core/shared/view/elements/PileWithActions/PileWithActions';
import PilePopup from 'core/shared/view/elements/PileWithActions/PipePopup/PipePopup';
import { checkSupportArtifactPreview } from 'features/artifactManager/store/helpers';
import {
  IArtifact,
  IArtifactWithDatasetVersion,
  checkArtifactWithDatasetVersion,
} from 'core/shared/models/Artifact';
import { IconType } from 'core/shared/view/elements/Icon/Icon';
import ArtifactPreview from 'features/artifactManager/view/ArtifactButton/ArtifactPreview/ArtifactPreview';
import { EntityType } from 'features/artifactManager/store';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import routes from 'routes';
import { useArtifactDatasetVersion } from 'features/artifactManager/store/artifactDatasetVersion';

export const useInfoAction = ({
  popupIconType,
  artifact,
  entityId,
  entityType,
}: {
  popupIconType: IconType;
  entityType: EntityType;
  entityId: string;
  artifact: IArtifact;
}) => {
  const [isShowPopupInfo, changeIsShowPopupInfo] = React.useState(false);

  return {
    isShowPreloader: false,
    content: (
      <>
        {isShowPopupInfo && (
          <InfoPopup
            onClose={() => changeIsShowPopupInfo(false)}
            artifact={artifact}
            entityType={entityType}
            entityId={entityId}
            iconType={popupIconType}
          />
        )}
        <Action
          iconType="preview"
          onClick={() => changeIsShowPopupInfo(true)}
        />
      </>
    ),
  };
};

const InfoPopup = ({
  artifact,
  iconType,
  entityId,
  entityType,
  onClose,
}: {
  iconType: IconType;
  entityType: EntityType;
  entityId: string;
  artifact: IArtifact;
  onClose: () => void;
}) => {
  const isSupportPreview = checkSupportArtifactPreview(artifact);
  return (
    <PilePopup
      title={
        isSupportPreview
          ? `Information and Preview for ${artifact.key}`
          : `Information for ${artifact.key}`
      }
      isOpen={true}
      titleIcon={iconType}
      onRequestClose={onClose}
    >
      <PilePopup.Fields>
        {() => (
          <>
            <PilePopup.Field label="Key">{artifact.key}</PilePopup.Field>
            <PilePopup.Field label="Type">{artifact.type}</PilePopup.Field>
            {checkArtifactWithDatasetVersion(artifact) ? (
              <DatasetVersionField artifact={artifact} />
            ) : null}
            <PilePopup.Field
              label={artifact.key === 'query' ? 'Query' : 'Path'}
            >
              {artifact.path}
            </PilePopup.Field>
            {isSupportPreview && (
              <ArtifactPreview
                artifact={artifact}
                entityId={entityId}
                entityType={entityType}
                isShowErrorIfExist={true}
                onClose={console.log}
              />
            )}
          </>
        )}
      </PilePopup.Fields>
    </PilePopup>
  );
};

const DatasetVersionField = ({
  artifact,
}: {
  artifact: IArtifactWithDatasetVersion;
}) => {
  const { datasetVersion, loadingDatasetVersion } = useArtifactDatasetVersion({
    artifact,
  });

  return (
    <PilePopup.Field label="Dataset version">
      {(() => {
        if (loadingDatasetVersion.isRequesting) {
          return <Preloader variant="dots" />;
        }
        if (loadingDatasetVersion.error || !datasetVersion) {
          return artifact.linkedArtifactId;
        }
        return (
          <Link
            to={routes.datasetVersion.getRedirectPathWithCurrentWorkspace({
              datasetId: datasetVersion.datasetId,
              datasetVersionId: datasetVersion.id,
            })}
          >
            {artifact.linkedArtifactId}
          </Link>
        );
      })()}
    </PilePopup.Field>
  );
};
