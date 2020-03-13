import * as React from 'react';

import ClientSuggestion from 'components/ModelRecordProps/shared/ClientSuggestion/ClientSuggestion';
import { IArtifact } from 'core/shared/models/Artifact';
import { ICommunication } from 'core/shared/utils/redux/communication';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import { EntityType } from 'store/artifactManager';

import ArtifactButton, {
  IDeleteArtifactInfo,
} from '../ArtifactButton/ArtifactButton';

import styles from '../../shared/sharedStyles.module.css';

type PillSize = 'small' | 'medium';

interface ILocalProps {
  entitiyId: string;
  entityType: EntityType;
  artifacts: IArtifact[];
  docLink?: string;
  pillSize?: PillSize;
  maxHeight?: number;
  getArtifactClassname?: (key: string) => string | undefined;
  deletingInfo?: {
    delete: IDeleteArtifactInfo['delete'];
    isCurrentUserCanDeleteArtifact: IDeleteArtifactInfo['isCurrentUserCanDeleteArtifact'];
    getDeleting: (artifactKey: string) => ICommunication;
  };
}

class Artifacts extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      artifacts,
      entitiyId,
      entityType,
      pillSize: size,
      maxHeight,
      docLink,
      getArtifactClassname = () => undefined,
      deletingInfo,
    } = this.props;

    return (
      <div data-test="artifacts">
        {artifacts.length !== 0 ? (
          <div
            className={size === 'medium' ? styles.record_pills_container : ''}
          >
            <ScrollableContainer
              maxHeight={maxHeight || 180}
              containerOffsetValue={12}
              children={
                <>
                  {artifacts.map((artifact: IArtifact, i: number) => (
                    <ArtifactButton
                      entityId={entitiyId}
                      entityType={entityType}
                      key={i}
                      additionalClassname={getArtifactClassname(artifact.key)}
                      artifact={artifact}
                      deleteInfo={
                        deletingInfo
                          ? {
                              delete: deletingInfo.delete,
                              deleting: deletingInfo.getDeleting(artifact.key),
                              isCurrentUserCanDeleteArtifact:
                                deletingInfo.isCurrentUserCanDeleteArtifact,
                            }
                          : undefined
                      }
                    />
                  ))}
                </>
              }
            />
          </div>
        ) : (
          docLink && (
            <ClientSuggestion
              fieldName={'artifact'}
              clientMethod={'log_artifact()'}
              link={docLink}
            />
          )
        )}
      </div>
    );
  }
}

export default Artifacts;
