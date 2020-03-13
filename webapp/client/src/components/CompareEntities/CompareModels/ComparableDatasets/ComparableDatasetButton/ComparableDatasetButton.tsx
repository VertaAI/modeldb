import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import {
  getDiffValueBgClassname,
  getDiffValueBorderClassname,
} from 'components/CompareEntities/shared/DiffHighlight/DiffHighlight';
import { IArtifact } from 'core/shared/models/Artifact';
import {
  ICommunication,
  initialCommunication,
  ICommunicationById,
} from 'core/shared/utils/redux/communication';
import Button from 'core/shared/view/elements/Button/Button';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import { IDatasetVersion } from 'models/DatasetVersion';
import routes from 'routes';
import {
  reset,
  loadDatasetVersion,
  selectCommunications,
  selectDatasetVersions,
} from 'store/artifactManager';
import { EntityType } from 'store/compareEntities';
import { IApplicationState } from 'store/store';

import { IComparedDataset } from '../ComparableDatasets';
import styles from './ComparableDatasetButton.module.css';

interface ILocalProps {
  comparedDataset: IComparedDataset;
  entityType: EntityType;
}

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

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class ArtifactButton extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
  };

  public componentDidUpdate(prevProps: AllProps, prevState: ILocalState) {
    if (!prevState.isModalOpen && this.state.isModalOpen) {
      if (this.props.comparedDataset.dataset.linkedArtifactId) {
        this.props.loadDatasetVersion(
          this.props.comparedDataset.dataset.linkedArtifactId
        );
      }
      if (
        this.props.comparedDataset.otherEntityDataset &&
        this.props.comparedDataset.otherEntityDataset.linkedArtifactId
      ) {
        this.props.loadDatasetVersion(
          this.props.comparedDataset.otherEntityDataset.linkedArtifactId
        );
      }
    }
  }

  public render() {
    const {
      comparedDataset,
      loadingDatasetVersions,
      datasetVersions,
      entityType,
    } = this.props;
    const { isModalOpen } = this.state;
    const iconType = (() => {
      if (comparedDataset.dataset.key === 'query') {
        return 'query';
      }
      if (comparedDataset.dataset.type === 'IMAGE') {
        return 'image';
      }
      if (comparedDataset.dataset.type === 'BINARY') {
        return 'cube';
      }
      return 'codepen';
    })();

    const dataset1 = (() => {
      if (entityType === EntityType.entity1) {
        return comparedDataset.dataset;
      }
      return comparedDataset.otherEntityDataset;
    })();
    const dataset2 = (() => {
      if (entityType === EntityType.entity1) {
        return comparedDataset.otherEntityDataset;
      }
      return comparedDataset.dataset;
    })();
    const datasetVersion1 =
      dataset1 && dataset1.linkedArtifactId
        ? datasetVersions[dataset1.linkedArtifactId]
        : undefined;
    const datasetVersion2 =
      dataset2 && dataset2.linkedArtifactId
        ? datasetVersions[dataset2.linkedArtifactId]
        : undefined;

    const buttonAdditionClassName = getDiffValueBorderClassname(
      entityType,
      comparedDataset.isDifferent
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
              {comparedDataset.dataset.key}
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
              <div className={styles.datasets}>
                <div
                  className={cn(styles.dataset, {
                    [styles.empty]: !Boolean(dataset1),
                  })}
                >
                  {dataset1
                    ? this.renderDataset({
                        dataset: dataset1,
                        entityType: EntityType.entity1,
                        otherDataset: dataset2,
                        loadingDatasetVersions: dataset1.linkedArtifactId
                          ? loadingDatasetVersions[dataset1.linkedArtifactId] ||
                            initialCommunication
                          : initialCommunication,
                        datasetVersion: dataset1.linkedArtifactId
                          ? datasetVersions[dataset1.linkedArtifactId]
                          : undefined,
                      })
                    : '-'}
                </div>
                <div
                  className={cn(styles.dataset, {
                    [styles.empty]: !Boolean(dataset2),
                  })}
                >
                  {dataset2
                    ? this.renderDataset({
                        dataset: dataset2,
                        otherDataset: dataset1,
                        entityType: EntityType.entity2,
                        loadingDatasetVersions: dataset2.linkedArtifactId
                          ? loadingDatasetVersions[dataset2.linkedArtifactId] ||
                            initialCommunication
                          : initialCommunication,
                        datasetVersion: dataset2.linkedArtifactId
                          ? datasetVersions[dataset2.linkedArtifactId]
                          : undefined,
                      })
                    : '-'}
                </div>
              </div>
              <div className={styles.compare}>
                <Button
                  disabled={
                    !datasetVersion1 ||
                    !datasetVersion2 ||
                    datasetVersion1.id === datasetVersion2.id
                  }
                  to={
                    datasetVersion1 &&
                    datasetVersion2 &&
                    datasetVersion1.datasetId === datasetVersion2.datasetId
                      ? routes.compareDatasetVersions.getRedirectPathWithCurrentWorkspace(
                          {
                            datasetId: datasetVersion1.datasetId,
                            datasetVersionId1: datasetVersion1.id,
                            datasetVersionId2: datasetVersion2.id,
                          }
                        )
                      : undefined
                  }
                >
                  Compare
                </Button>
              </div>
            </div>
          </Popup>
        )}
      </div>
    );
  }

  @bind
  private renderDataset(props: {
    dataset: IArtifact;
    otherDataset: IArtifact | undefined;
    entityType: EntityType;
    loadingDatasetVersions: ICommunication;
    datasetVersion: IDatasetVersion | undefined;
  }) {
    const {
      dataset,
      otherDataset,
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
            otherDataset
              ? getDiffValueBgClassname(
                  entityType,
                  dataset.key !== otherDataset.key
                )
              : undefined
          }
        />
        <this.RenderField
          keystr="Type"
          value={dataset.type}
          additionalValueClassname={
            otherDataset
              ? getDiffValueBgClassname(
                  entityType,
                  dataset.type !== otherDataset.type
                )
              : undefined
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
              otherDataset
                ? getDiffValueBgClassname(
                    entityType,
                    dataset.linkedArtifactId !== otherDataset.linkedArtifactId
                  )
                : undefined
            }
          />
        ) : null}
        {dataset.key === 'query' ? (
          <this.RenderPathField
            keystr="Query"
            value={dataset.path}
            query={true}
            additionalValueClassname={
              otherDataset
                ? getDiffValueBgClassname(
                    entityType,
                    dataset.key !== otherDataset.key
                  )
                : undefined
            }
          />
        ) : (
          <this.RenderPathField
            keystr="Path"
            value={dataset.path}
            additionalValueClassname={
              otherDataset
                ? getDiffValueBgClassname(
                    entityType,
                    dataset.key !== otherDataset.key
                  )
                : undefined
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
)(ArtifactButton);
