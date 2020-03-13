import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps, withRouter } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import { Artifact, checkArtifactWithPath } from 'core/shared/models/Artifact';
import { artifactErrorMessages } from 'core/shared/utils/customErrorMessages';
import {
  ICommunication,
  initialCommunication,
} from 'core/shared/utils/redux/communication';
import Button from 'core/shared/view/elements/Button/Button';
import ButtonLikeText from 'core/shared/view/elements/ButtonLikeText/ButtonLikeText';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { IDatasetVersion } from 'models/DatasetVersion';
import routes, { GetRouteParams } from 'routes';
import {
  downloadArtifact,
  reset,
  EntityType,
  loadDatasetVersion,
  selectCommunications,
  selectDatasetVersion,
  selectDownloadingArtifact,
  selectLoadingArtifactPreview,
} from 'store/artifactManager';
import { checkSupportArtifactPreview } from 'store/artifactManager/helpers';
import { IApplicationState } from 'store/store';

import DownloadArtifactButton from '../DownloadArtifactButton/DownloadArtifactButton';
import styles from './ArtifactButton.module.css';
import ArtifactPreview from './ArtifactPreview/ArtifactPreview';
import { AppError } from 'core/shared/models/Error';
import LastCommunicationError from 'core/shared/view/elements/LastCommunicationError/LastCommunicationError';

interface ILocalProps {
  buttonTitle?: string;
  entityType: EntityType;
  entityId: string;
  artifact: Artifact;
  additionalClassname?: string;
  deleteInfo?: IDeleteArtifactInfo;
}

interface IPropsFromState {
  datasetVersion: IDatasetVersion | null;
  downloadingArtifact: ICommunication;
  loadingDatasetVersions: ICommunication;
  loadingArtifactPreview: ICommunication;
}

interface IActionProps {
  loadDatasetVersion: typeof loadDatasetVersion;
  reset: typeof reset;
}

interface ILocalState {
  isModalOpen: boolean;
  isShownPreview: boolean;
}

type AllProps = ILocalProps &
  IPropsFromState &
  IActionProps &
  RouteComponentProps<GetRouteParams<typeof routes.workspace>>;

export interface IDeleteArtifactInfo {
  delete: (entityId: string, artifactKey: string) => void;
  deleting: ICommunication;
  isCurrentUserCanDeleteArtifact: boolean;
}

class ArtifactButton extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
    isShownPreview: false,
  };

  public componentDidUpdate(prevProps: AllProps, prevState: ILocalState) {
    if (
      !prevState.isModalOpen &&
      this.state.isModalOpen &&
      this.props.artifact.linkedArtifactId
    ) {
      this.props.loadDatasetVersion(
        this.props.match.params.workspaceName,
        this.props.artifact.linkedArtifactId
      );
    }
  }

  public render() {
    const {
      artifact,
      additionalClassname,
      entityId,
      entityType,
      loadingDatasetVersions,
      datasetVersion,
      buttonTitle,
      deleteInfo,
    } = this.props;
    const { isShownPreview, isModalOpen } = this.state;
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

    return (
      <div>
        <div
          className={cn(styles.model_link, styles.artifact_item, {
            [styles.deleting]: Boolean(
              deleteInfo && deleteInfo.deleting.isRequesting
            ),
          })}
          onClick={this.showModal}
        >
          <div
            className={cn(styles.artifact_wrapper, additionalClassname)}
            title="view artifact"
            data-test="artifact"
          >
            <div className={styles.notif}>
              <Icon className={styles.notif_icon} type={iconType} />
            </div>
            <div className={styles.artifactKey} data-test="artifact-key">
              {buttonTitle || artifact.key}
            </div>
          </div>
        </div>

        {isModalOpen && (
          <Popup
            title={'Artifact'}
            titleIcon={iconType}
            contentLabel="artifact-action"
            isOpen={true}
            onRequestClose={this.closeModal}
          >
            <div
              className={cn(styles.popupContent, {
                [styles.deleting]: Boolean(
                  deleteInfo && deleteInfo.deleting.isRequesting
                ),
              })}
            >
              <div>
                <this.RenderField keystr="Key" value={artifact.key} />
                <this.RenderField keystr="Type" value={artifact.type} />
                {artifact.linkedArtifactId ? (
                  <this.RenderField
                    keystr="Dataset version"
                    value={(() => {
                      if (loadingDatasetVersions.isRequesting) {
                        return <Preloader variant="dots" />;
                      }
                      if (loadingDatasetVersions.error || !datasetVersion) {
                        return artifact.linkedArtifactId;
                      }
                      return (
                        <Link
                          to={routes.datasetVersion.getRedirectPathWithCurrentWorkspace(
                            {
                              datasetId: datasetVersion.datasetId,
                              datasetVersionId: datasetVersion.id,
                            }
                          )}
                        >
                          {artifact.linkedArtifactId}
                        </Link>
                      );
                    })()}
                  />
                ) : null}
                {artifact.key === 'query' ? (
                  <this.RenderPathField
                    keystr="Query"
                    value={artifact.path}
                    query={true}
                  />
                ) : (
                  <this.RenderPathField
                    keystr="Path"
                    value={artifact.path}
                    additionalContent={
                      checkSupportArtifactPreview(artifact) ? (
                        <ButtonLikeText onClick={this.showPreview}>
                          preview
                        </ButtonLikeText>
                      ) : (
                        undefined
                      )
                    }
                  />
                )}
                <div className={styles.preview}>
                  {isShownPreview && (
                    <ArtifactPreview
                      entityType={entityType}
                      entityId={entityId}
                      artifact={artifact}
                      onClose={this.closePreview}
                    />
                  )}
                </div>
                <this.LastCommunicationErrorIfExist />
                {checkArtifactWithPath(artifact) && (
                  <div className={styles.popupActions}>
                    <div className={styles.popupAction}>
                      <div className={styles.downloadActionBlock}>
                        <DownloadArtifactButton
                          isShowErrorIfExist={false}
                          artifact={artifact as any}
                          entityId={this.props.entityId}
                          entityType={this.props.entityType}
                        />
                      </div>
                    </div>
                    {deleteInfo && deleteInfo.isCurrentUserCanDeleteArtifact && (
                      <div className={styles.popupAction}>
                        <div className={styles.deletingArtifactActionBlock}>
                          <Button theme="red" onClick={this.deleteArtifact}>
                            Delete Artifact
                          </Button>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            </div>
          </Popup>
        )}
      </div>
    );
  }

  @bind
  private LastCommunicationErrorIfExist() {
    const handledCommunicationsDesc: Array<{
      communication: ICommunication<any>;
      render: (error: AppError<any>) => React.ReactNode;
    }> = [
      {
        communication: this.props.downloadingArtifact,
        render: (error: AppError) => (
          <InlineCommunicationError
            error={error}
            customMessage={artifactErrorMessages.artifact_download}
          />
        ),
      },
      {
        communication: this.props.loadingArtifactPreview,
        render: (error: AppError) => (
          <InlineCommunicationError
            error={error}
            customMessage={artifactErrorMessages.artifact_preview}
          />
        ),
      },
      this.props.deleteInfo
        ? {
            communication: this.props.deleteInfo.deleting,
            render: (error: AppError) => (
              <InlineCommunicationError
                customMessage={artifactErrorMessages.artifact_deleting}
                error={error}
              />
            ),
          }
        : null,
    ].filter(isNotNill);
    return (
      <LastCommunicationError
        communications={handledCommunicationsDesc.map(
          ({ communication }) => communication
        )}
      >
        {(error, lastFailedCommunication) => {
          const targetCommunicationDesc = handledCommunicationsDesc.find(
            ({ communication }) => communication === lastFailedCommunication
          );
          return targetCommunicationDesc
            ? targetCommunicationDesc.render(error)
            : null;
        }}
      </LastCommunicationError>
    );
  }

  @bind
  private showPreview() {
    this.setState({ isShownPreview: true });
  }
  @bind
  private closePreview() {
    this.setState({ isShownPreview: false });
  }

  @bind
  private deleteArtifact() {
    if (this.props.deleteInfo) {
      this.props.deleteInfo.delete(
        this.props.entityId,
        this.props.artifact.key
      );
    }
  }

  @bind
  private closeModal() {
    this.setState({ isModalOpen: false, isShownPreview: false });
    this.props.reset();
  }
  @bind
  private showModal() {
    this.setState({ isModalOpen: true });
  }

  private RenderField(props: { keystr: string; value: any }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value && <div className={styles.fieldValue}>{value}</div>}
      </div>
    );
  }

  private RenderPathField(props: {
    keystr?: string;
    value?: string | number;
    query?: boolean;
    additionalContent?: React.ReactNode;
  }) {
    const { keystr, value, query, additionalContent } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value && query ? (
          <div className={styles.pathFieldValueQuery}>{value}</div>
        ) : (
          <div className={styles.pathFieldValue}>{value}</div>
        )}
        <div className={styles.additionalContent}>{additionalContent}</div>
      </div>
    );
  }
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      downloadArtifact,
      reset,
      loadDatasetVersion,
    },
    dispatch
  );
};

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    loadingDatasetVersions: localProps.artifact.linkedArtifactId
      ? selectCommunications(state).loadingDatasetVersions[
          localProps.artifact.linkedArtifactId
        ] || initialCommunication
      : initialCommunication,
    datasetVersion: localProps.artifact.linkedArtifactId
      ? selectDatasetVersion(state, localProps.artifact.linkedArtifactId) ||
        null
      : null,
    downloadingArtifact: selectDownloadingArtifact(state),
    loadingArtifactPreview: selectLoadingArtifactPreview(state),
  };
};

function isNotNill<T>(elem: T | null | undefined): elem is T {
  return elem !== null && elem !== undefined;
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(ArtifactButton));
