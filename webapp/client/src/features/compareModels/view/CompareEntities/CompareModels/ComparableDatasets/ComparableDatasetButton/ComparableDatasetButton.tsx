import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link, RouteComponentProps, withRouter } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import {
  getDiffValueBorderClassname,
  getDiffValueBgClassname,
} from '../../../shared/DiffHighlight/DiffHighlight';
import { IArtifact } from 'shared/models/Artifact';
import {
  ICommunication,
  initialCommunication,
  ICommunicationById,
} from 'shared/utils/redux/communication';
import Button from 'shared/view/elements/Button/Button';
import { Icon } from 'shared/view/elements/Icon/Icon';
import Popup from 'shared/view/elements/Popup/Popup';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import { IDatasetVersion } from 'shared/models/DatasetVersion';
import routes, { GetRouteParams } from 'shared/routes';
import {
  reset,
  loadDatasetVersion,
  selectCommunications,
  selectDatasetVersions,
} from 'features/artifactManager/store';
import { IApplicationState } from 'setup/store/store';
import { oneOfKeyIsDiff, IDatasetDiff } from '../../../../../store/compareModels/compareModels';

import styles from './ComparableDatasetButton.module.css';
import { PopupComparedEntities } from '../../../shared/PopupComparedEntities/PopupComparedEntities';

interface ILocalProps {
  currentDataset: IModelAttributeByKey;
  modelsDatasetsByKey: IModelsAttributesByKey;
}

export type IModelsAttributesByKey = Array<IModelAttributeByKey | undefined>; // todo rename
export type IModelAttributeByKey = {
  modelNumber: number;
  diff: IDatasetDiff;
  dataset: IArtifact;
};

interface IPropsFromState {
  datasetVersions: Record<string, IDatasetVersion | undefined>;
  loadingDatasetVersions: ICommunicationById;
}

interface IActionProps {
  loadDatasetVersion: typeof loadDatasetVersion;
  reset: typeof reset;
}

interface ILocalState {
  isModalOpen: boolean;
}

type AllProps = ILocalProps &
  IPropsFromState &
  IActionProps &
  RouteComponentProps<GetRouteParams<typeof routes.workspace>>;

class ArtifactButton extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
  };

  public componentDidUpdate(prevProps: AllProps, prevState: ILocalState) {
    if (!prevState.isModalOpen && this.state.isModalOpen) {
      if (this.props.currentDataset.dataset.linkedArtifactId) {
        this.props.loadDatasetVersion(
          this.props.match.params.workspaceName,
          this.props.currentDataset.dataset.linkedArtifactId
        );
      }
      this.props.modelsDatasetsByKey
        .filter((m) => m && m.dataset.linkedArtifactId && m.dataset.linkedArtifactId !== this.props.currentDataset.dataset.linkedArtifactId)
        .forEach((m) => {
          // todo add loadDatasetVersions 
          this.props.loadDatasetVersion(
            this.props.match.params.workspaceName,
            m!.dataset.linkedArtifactId!,
          );
        });
    }
  }

  public render() {
    const {
      currentDataset,
      loadingDatasetVersions,
      datasetVersions,
      modelsDatasetsByKey
    } = this.props;
    const { isModalOpen } = this.state;
    const iconType = (() => {
      if (currentDataset.dataset.key === 'query') {
        return 'query';
      }
      if (currentDataset.dataset.type === 'IMAGE') {
        return 'image';
      }
      if (currentDataset.dataset.type === 'BINARY') {
        return 'cube';
      }
      return 'codepen';
    })();

    const buttonAdditionClassName = getDiffValueBorderClassname(
      currentDataset.modelNumber,
      oneOfKeyIsDiff(currentDataset.diff),
    );

    return (
      <div>
        <div
          className={cn(styles.model_link, styles.artifact_item)}
          onClick={this.showModal}
        >
          <div
            className={cn(styles.artifact_wrapper, buttonAdditionClassName)}
            title="view artifact"
            data-test="artifact"
          >
            <div className={styles.notif}>
              <Icon className={styles.notif_icon} type={iconType} />
            </div>
            <div className={styles.artifactKey} data-test="artifact-key">
              {currentDataset.dataset.key}
            </div>
          </div>
        </div>

        {isModalOpen && (
          <Popup
            title={'Comparing datasets'}
            titleIcon={iconType}
            contentLabel="artifact-action"
            isOpen={true}
            size="large"
            onRequestClose={this.closeModal}
          >
            <div className={cn(styles.popupContent)}>
              <PopupComparedEntities entities={modelsDatasetsByKey}>
                {(datasetInfo) => (
                  <div className={styles.dataset}>
                    {this.renderDataset({
                      dataset: datasetInfo.dataset,
                      entityType: datasetInfo.modelNumber,
                      diffInfo: datasetInfo.diff,
                      loadingDatasetVersions: loadingDatasetVersions[datasetInfo.dataset.linkedArtifactId || ''] || initialCommunication,
                      datasetVersion: datasetVersions[datasetInfo.dataset.linkedArtifactId || ''],
                    })}
                  </div>
                )}
              </PopupComparedEntities>
              {(() => {
                const [dataset1, dataset2] = modelsDatasetsByKey;
                const datasetVersion1 = datasetVersions[dataset1?.dataset?.linkedArtifactId || ''];
                const datasetVersion2 = datasetVersions[dataset2?.dataset?.linkedArtifactId || ''];
                return modelsDatasetsByKey.length === 2 && datasetVersion1 && datasetVersion2 && datasetVersion1.datasetId === datasetVersion2.datasetId && (
                  <div className={styles.compare}>
                    <Button
                      to={
                        routes.compareDatasetVersions.getRedirectPathWithCurrentWorkspace(
                          {
                            datasetId: datasetVersion1.datasetId,
                            datasetVersionId1: datasetVersion1.id,
                            datasetVersionId2: datasetVersion2.id,
                          }
                        )
                      }
                    >
                      Compare
                    </Button>
                  </div>
                );
              })()}
            </div>
          </Popup>
        )}
      </div>
    );
  }

  @bind
  private renderDataset(props: {
    dataset: IArtifact;
    diffInfo: IDatasetDiff;
    entityType: number;
    loadingDatasetVersions: ICommunication;
    datasetVersion: IDatasetVersion | undefined;
  }) {
    const {
      dataset,
      diffInfo,
      entityType,
      loadingDatasetVersions,
      datasetVersion,
    } = props;
    return (
      <div>
        <this.RenderField
          keystr="Key"
          value={dataset.key}
          additionalValueClassname={
            getDiffValueBgClassname(
              entityType,
              diffInfo['key'],
            )
          }
        />
        <this.RenderField
          keystr="Type"
          value={dataset.type}
          additionalValueClassname={
            getDiffValueBgClassname(
              entityType,
              diffInfo['type'],
            )
          }
        />
        {dataset.linkedArtifactId ? (
          <this.RenderField
            keystr="Dataset version"
            value={(() => {
              if (loadingDatasetVersions.isRequesting) {
                return <Preloader variant="dots" />;
              }
              if (loadingDatasetVersions.error || !datasetVersion) {
                return dataset.linkedArtifactId;
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
                  {dataset.linkedArtifactId}
                </Link>
              );
            })()}
            additionalValueClassname={
              getDiffValueBgClassname(
                entityType,
                diffInfo['linkedArtifactId']
              )
            }
          />
        ) : null}
        {dataset.key === 'query' ? (
          <this.RenderPathField
            keystr="Query"
            value={dataset.path}
            query={true}
            additionalValueClassname={
              getDiffValueBgClassname(
                entityType,
                diffInfo['path']
              )
            }
          />
        ) : (
            <this.RenderPathField
              keystr="Path"
              value={dataset.path}
              additionalValueClassname={
                getDiffValueBgClassname(
                  entityType,
                  diffInfo['path']
                )
              }
            />
          )}
      </div>
    );
  }

  @bind
  private closeModal() {
    this.setState({ isModalOpen: false });
    this.props.reset();
  }
  @bind
  private showModal() {
    this.setState({ isModalOpen: true });
  }

  private RenderField(props: {
    keystr: string;
    value: any;
    additionalValueClassname?: string | undefined;
  }) {
    const { keystr, value, additionalValueClassname } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value && (
          <div className={cn(styles.fieldValue, additionalValueClassname)}>
            {value}
          </div>
        )}
      </div>
    );
  }

  private RenderPathField(props: {
    keystr?: string;
    value?: string | number;
    query?: boolean;
    additionalValueClassname?: string | undefined;
  }) {
    const { keystr, value, query, additionalValueClassname } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value && query ? (
          <div
            className={cn(styles.pathFieldValueQuery, additionalValueClassname)}
          >
            {value}
          </div>
        ) : (
            <div className={cn(styles.pathFieldValue, additionalValueClassname)}>
              {value}
            </div>
          )}
      </div>
    );
  }
}

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      reset,
      loadDatasetVersion,
    },
    dispatch
  );
};

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    loadingDatasetVersions: selectCommunications(state).loadingDatasetVersions,
    datasetVersions: selectDatasetVersions(state),
  };
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(withRouter(ArtifactButton));
