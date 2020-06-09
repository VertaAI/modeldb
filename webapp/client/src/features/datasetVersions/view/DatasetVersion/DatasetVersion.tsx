import cn from 'classnames';
import { bind } from 'decko';
import React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { formatBytes } from 'shared/utils/mapperConverters/DataSizeConverted';
import { initialCommunication } from 'shared/utils/redux/communication';
import { matchRemoteData } from 'shared/utils/redux/communication/remoteData';
import removeQuotes from 'shared/utils/removeQuotes';
import DataSourceUri from 'shared/view/domain/DatasetVersionProps/QueryDatasetVersionProps/DataSourceUri/DataSourceUri';
import Attributes from 'shared/view/domain/ModelRecord/ModelRecordProps/Attributes/Attributes/Attributes';
import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import CopyButton from 'shared/view/elements/CopyButton/CopyButton';
import DeleteFAI from 'shared/view/elements/DeleteFAI/DeleteFAI';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import IdView from 'shared/view/elements/IdView/IdView';
import { PageCard } from 'shared/view/elements/PageComponents';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import {
  loadDatasetVersion,
  deleteDatasetVersion,
  selectCommunications,
  selectDatasetVersion,
  selectDeletingDatasetVersion,
  loadDatasetVersionExperimentRuns,
  selectDatasetVersionExperimentRuns,
} from '../../store';
import DatasetEntityDescriptionManager from 'features/descriptionManager/view/DatasetEntityDescriptionManager/DatasetEntityDescriptionManager';
import DatasetEntityTagsManager from 'features/tagsManager/view/DatasetEntityTagsManager/DatasetEntityTagsManager';
import {
  IRawDatasetVersionInfo,
  IPathBasedDatasetVersionInfo,
  IQueryDatasetVersionInfo,
} from 'shared/models/DatasetVersion';
import routes from 'shared/routes';
import { IApplicationState } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import DatasetPathInfoTable from './DatasetPathInfoTable/DatasetPathInfoTable';
import styles from './DatasetVersion.module.css';
import DatasetVersionExperimentRuns from './DatasetVersionExperimentRuns/DatasetVersionExperimentRuns';
import { selectDataset } from 'features/datasets/store';
import { pathLocationLabels } from '../shared/constants';

const mapStateToProps = (state: IApplicationState, localProps: ILocalProps) => {
  const datasetVersion = selectDatasetVersion(
    state,
    localProps.datasetVersionId
  );
  return {
    loadingDatasetVersion: selectCommunications(state).loadingDatasetVersion,
    datasetVersion,
    dataset: selectDataset(state, localProps.datasetId),

    experimentRuns: selectDatasetVersionExperimentRuns(
      state,
      localProps.datasetVersionId
    ),
    loadingDatasetVersionExperimentRuns:
      selectCommunications(state).loadDatasetVersionExperimentRuns[
        localProps.datasetVersionId
      ] || initialCommunication,

    deleting: datasetVersion
      ? selectDeletingDatasetVersion(state, datasetVersion.id)
      : initialCommunication,

    workspaceName: selectCurrentWorkspaceName(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      loadDatasetVersion,
      deleteDatasetVersion,
      loadDatasetVersionExperimentRuns,
    },
    dispatch
  );
};

interface ILocalProps {
  datasetId: string;
  datasetVersionId: string;
}

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  RouteComponentProps;

class DatasetVersion extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.props.loadDatasetVersion(
      this.props.workspaceName,
      this.props.datasetVersionId,
      this.props.datasetId
    );
    this.props.loadDatasetVersionExperimentRuns(
      this.props.workspaceName,
      this.props.datasetVersionId
    );
  }

  public componentDidUpdate(prevProps: AllProps) {
    if (prevProps.deleting.isRequesting && !this.props.deleting.isRequesting) {
      this.props.history.replace(
        routes.datasetVersions.getRedirectPathWithCurrentWorkspace({
          datasetId: this.props.datasetId,
        })
      );
    }
  }

  public render() {
    const {
      loadingDatasetVersion,
      datasetVersion,
      deleting,
      dataset,
      experimentRuns,
      loadingDatasetVersionExperimentRuns,
      workspaceName,
    } = this.props;

    return (
      <div
        className={cn(styles.root, {
          [styles.deleting]: deleting.isRequesting,
        })}
      >
        {(() => {
          if (loadingDatasetVersion.isRequesting) {
            return (
              <div className={styles.preloader}>
                <Preloader variant="dots" />
              </div>
            );
          }
          if (loadingDatasetVersion.error || !datasetVersion) {
            return (
              <PageCommunicationError error={loadingDatasetVersion.error} />
            );
          }
          return (
            <PageCard dataTest="dataset-version">
              <div className={styles.summary}>
                <div className={styles.summary_main}>
                  <div
                    className={styles.id}
                    title={
                      datasetVersion.version
                        ? 'Version' + datasetVersion.version
                        : 'Version 1'
                    }
                    data-test="dataset-version-name"
                  >
                    {datasetVersion.version
                      ? 'Version' + datasetVersion.version
                      : 'Version 1'}
                  </div>
                  <div className={styles.description}>
                    <DatasetEntityDescriptionManager
                      entityType="datasetVersion"
                      entityId={datasetVersion.id}
                      datasetId={datasetVersion.datasetId}
                      description={datasetVersion.description}
                    />
                  </div>
                  <div className={styles.tags}>
                    <DatasetEntityTagsManager
                      id={datasetVersion.id}
                      datasetId={datasetVersion.datasetId}
                      entityType="datasetVersion"
                      tags={datasetVersion.tags}
                      isDraggableTags={true}
                    />
                  </div>
                </div>
                <div className={styles.summary_meta}>
                  {datasetVersion.parentId && (
                    <this.SummaryMetaRecord
                      label="ParentId"
                      title={datasetVersion.parentId}
                    >
                      <IdView value={datasetVersion.parentId} />
                    </this.SummaryMetaRecord>
                  )}
                  <this.SummaryMetaRecord label="Type">
                    {datasetVersion.type ? (
                      <span className={styles.location_type_value}>
                        {datasetVersion.type}
                      </span>
                    ) : (
                      '-'
                    )}
                  </this.SummaryMetaRecord>
                  {datasetVersion.type === 'path' && (
                    <this.SummaryMetaRecord label="Location">
                      <span>
                        {datasetVersion.info.locationType ? (
                          <span className={styles.dataset_location_value}>
                            {
                              pathLocationLabels[
                                datasetVersion.info.locationType
                              ]
                            }
                          </span>
                        ) : datasetVersion.info.basePath ? (
                          <span className={styles.dataset_location_value}>
                            {pathLocationLabels.localFileSystem}
                          </span>
                        ) : (
                          '-'
                        )}
                      </span>
                    </this.SummaryMetaRecord>
                  )}
                  <this.SummaryMetaRecord label="Dataset Name">
                    <div className={styles.dataset}>
                      {dataset && (
                        <>
                          <div
                            className={styles.dataset_name}
                            title={dataset.name}
                          >
                            {dataset && dataset.name}
                          </div>
                          <div className={styles.dataset_id}>
                            (<IdView value={dataset.id} sliceStringUpto={7} />
                            , <CopyButton value={dataset.id} />)
                          </div>
                        </>
                      )}
                    </div>
                  </this.SummaryMetaRecord>
                  <WithCurrentUserActionsAccesses
                    entityType="datasetVersion"
                    entityId={datasetVersion.id}
                    actions={['delete']}
                  >
                    {({ actionsAccesses }) =>
                      actionsAccesses.delete && (
                        <this.SummaryMetaRecord label="Delete">
                          <DeleteFAI
                            faiDataTest="delete-dataset-version-button"
                            onDelete={this.deleteDatasetVersion}
                            confirmText={<>Are you sure?</>}
                          />
                        </this.SummaryMetaRecord>
                      )
                    }
                  </WithCurrentUserActionsAccesses>
                </div>
              </div>
              <this.Record label="Attributes">
                {datasetVersion.attributes.length > 0 ? (
                  <Attributes attributes={datasetVersion.attributes} />
                ) : (
                  '-'
                )}
              </this.Record>

              {(() => {
                if (datasetVersion.type === 'raw') {
                  return <this.RawDatasetInfo info={datasetVersion.info} />;
                }
                if (datasetVersion.type === 'path') {
                  return <this.PathDatasetInfo info={datasetVersion.info} />;
                }
                if (datasetVersion.type === 'query') {
                  return <this.QueryDatasetInfo info={datasetVersion.info} />;
                }
              })()}
              <this.Record label="Timestamp">
                {datasetVersion.dateLogged ? (
                  <span>{getFormattedDateTime(datasetVersion.dateLogged)}</span>
                ) : (
                  '-'
                )}
              </this.Record>
              <this.Record
                label="Experiment Runs Using this Dataset Version:"
                isVertical={true}
              >
                {matchRemoteData(
                  loadingDatasetVersionExperimentRuns,
                  experimentRuns,
                  {
                    notAsked: () => (
                      <div className={styles.preloader}>
                        <Preloader variant="dots" />
                      </div>
                    ),
                    requesting: () => (
                      <div className={styles.preloader}>
                        <Preloader variant="dots" />
                      </div>
                    ),
                    errorOrNillData: ({ error, isNillData }) => (
                      <InlineCommunicationError
                        error={error}
                        isNillEntity={isNillData}
                      />
                    ),
                    success: loadedExperimentRuns =>
                      loadedExperimentRuns.length > 0 ? (
                        <div className={styles.experiment_runs}>
                          <DatasetVersionExperimentRuns
                            data={loadedExperimentRuns}
                            isCompacted={false}
                            workspaceName={workspaceName}
                          />
                        </div>
                      ) : (
                        '-'
                      ),
                  }
                )}
              </this.Record>
            </PageCard>
          );
        })()}
      </div>
    );
  }

  @bind
  private SummaryMetaRecord({
    label,
    children,
    title,
  }: {
    label: string;
    children: React.ReactNode;
    title?: string;
  }) {
    return (
      <div className={styles.summary_meta_record}>
        <div className={styles.summary_meta_record_label} title={label}>
          {label}
        </div>
        <div className={styles.summary_meta_record_value} title={title}>
          {children}
        </div>
      </div>
    );
  }

  @bind
  private Record({
    label,
    isVertical,
    children,
  }: {
    label: string;
    isVertical?: boolean;
    children: React.ReactNode;
  }) {
    return (
      <div
        className={cn(styles.record, { [styles.record_vertical]: isVertical })}
      >
        <div className={styles.record_label}>{label}</div>
        <div className={styles.record_value}>{children}</div>
      </div>
    );
  }

  @bind
  private RawDatasetInfo({ info }: { info: IRawDatasetVersionInfo }) {
    return (
      <>
        <this.Record label="Record Count">
          {safeMap(info.numRecords, v => v)}
        </this.Record>
        <this.Record label="Size">
          {safeMap(info.size, formatBytes)}
        </this.Record>
        <this.Record label="Object Path">
          <span className={styles.monospace_text}>
            {safeMap(info.objectPath, v => v)}
          </span>
        </this.Record>
        <this.Record label="Checksum">
          <div className={styles.dataset_path_checksum}>
            {safeMap(info.checkSum, checkSum => (
              <span>
                <IdView value={removeQuotes(checkSum)} sliceStringUpto={7} />
                ... <CopyButton value={removeQuotes(checkSum)} />
              </span>
            ))}
          </div>
        </this.Record>
        <this.Record label="Features">
          {info.features.length > 0 ? (
            <ScrollableContainer
              listContent={info.features}
              maxHeight={150}
              containerOffsetValue={20}
            />
          ) : (
            '-'
          )}
        </this.Record>
      </>
    );
  }

  @bind
  private QueryDatasetInfo({ info }: { info: IQueryDatasetVersionInfo }) {
    return (
      <>
        <this.Record label="Record Count">
          {safeMap(info.numRecords, v => v)}
        </this.Record>
        <this.Record label="Data Source URL">
          {safeMap(info.dataSourceUri, v => (
            <DataSourceUri value={v} />
          ))}
        </this.Record>
        <this.Record label="Query">
          <span className={styles.monospace_text}>
            {safeMap(info.query, v => v)}
          </span>
        </this.Record>
        <this.Record label="Query parameters">
          {info.queryParameters.length > 0 ? (
            <div className={styles.array_value}>
              {info.queryParameters.map(queryParameter => (
                <div key={queryParameter.name}>
                  <span className={styles.monospace_text}>
                    {JSON.stringify(queryParameter)}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            '-'
          )}
        </this.Record>
        <this.Record label="Query Template">
          <span className={styles.monospace_text}>
            {safeMap(info.queryTemplate, v => v)}
          </span>
        </this.Record>
      </>
    );
  }

  @bind
  private PathDatasetInfo({ info }: { info: IPathBasedDatasetVersionInfo }) {
    return (
      <>
        <this.Record label="Size">
          {safeMap(info.size, formatBytes)}
        </this.Record>
        <this.Record label="Base Path">
          <span className={styles.monospace_text}>
            {info.locationType === 's3FileSystem'
              ? safeMap(info.basePath, v => 's3://' + v)
              : safeMap(info.basePath, v => v)}
          </span>
        </this.Record>
        <this.Record label="Dataset Path Info">
          {info.datasetPathInfos.length > 0 ? (
            <div className={styles.path_info_grid_wrapper}>
              <DatasetPathInfoTable rows={info.datasetPathInfos} />
            </div>
          ) : (
            '-'
          )}
        </this.Record>
      </>
    );
  }

  @bind
  private deleteDatasetVersion() {
    if (this.props.datasetVersion) {
      this.props.deleteDatasetVersion(
        this.props.datasetVersion.datasetId,
        this.props.datasetVersion.id
      );
    }
  }
}

function safeMap<T, B>(
  val: T | string | null | undefined,
  f: (val: Exclude<T | string, undefined | null>) => B
): B | string {
  return val === null || val === undefined || val === ''
    ? '-'
    : f(val as Exclude<T | string, undefined | null>);
}

export default withRouter(
  connect(
    mapStateToProps,
    mapDispatchToProps
  )(DatasetVersion)
);
