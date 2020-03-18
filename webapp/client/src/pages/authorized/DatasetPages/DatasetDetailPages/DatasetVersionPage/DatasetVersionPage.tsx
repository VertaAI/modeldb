import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import DataSourceUri from 'core/shared/view/domain/DatasetVersionProps/QueryDatasetVersionProps/DataSourceUri/DataSourceUri';
import DatasetEntityDescriptionManager from 'components/DescriptionManager/DatasetEntityDescriptionManager/DatasetEntityDescriptionManager';
import Attributes from 'components/ModelRecordProps/Attributes/Attributes/Attributes';
import DatasetEntityTagsManager from 'components/TagsManager/DatasetEntityTagsManager/DatasetEntityTagsManager';
import { handleCustomErrorWithFallback } from 'core/shared/models/Error';
import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { formatBytes } from 'core/shared/utils/mapperConverters/DataSizeConverted';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import { matchRemoteData } from 'core/shared/utils/redux/communication/remoteData';
import removeQuotes from 'core/shared/utils/removeQuotes';
import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import DeleteFAI from 'core/shared/view/elements/DeleteFAI/DeleteFAI';
import InlineCommunicationError from 'core/shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import IdView from 'core/shared/view/elements/IdView/IdView';
import { PageCard } from 'core/shared/view/elements/PageComponents';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import ScrollableContainer from 'core/shared/view/elements/ScrollableContainer/ScrollableContainer';
import {
  IRawDatasetVersionInfo,
  IPathBasedDatasetVersionInfo,
  IQueryDatasetVersionInfo,
} from 'models/DatasetVersion';
import NotFoundPage from 'pages/authorized/NotFoundPage/NotFoundPage';
import AuthorizedLayout from 'pages/authorized/shared/AuthorizedLayout/AuthorizedLayout';
import routes, { GetRouteParams } from 'routes';
import { selectDataset } from 'store/datasets';
import {
  loadDatasetVersion,
  deleteDatasetVersion,
  selectCommunications,
  selectDatasetVersion,
  selectDeletingDatasetVersion,
  loadDatasetVersionExperimentRuns,
  selectDatasetVersionExperimentRuns,
} from 'store/datasetVersions';
import { IApplicationState } from 'store/store';
import { selectCurrentWorkspaceNameOrDefault } from 'store/workspaces';

import DatasetsPagesLayout from '../../shared/DatasetsPagesLayout/DatasetsPagesLayout';
import { pathLocationLabels } from '../shared/constants';
import DatasetPathInfoTable from '../shared/DatasetPathInfoTable/DatasetPathInfoTable';
import DatasetVersionExperimentRuns from './DatasetVersionExperimentRuns/DatasetVersionExperimentRuns';
import styles from './DatasetVersionPage.module.css';

interface ILocalProps {
  onShowNotFoundPage(): void; // todo check
}

const mapStateToProps = (state: IApplicationState, routeProps: RouteProps) => {
  const datasetVersion = selectDatasetVersion(
    state,
    routeProps.match.params.datasetVersionId
  );
  return {
    loadingDatasetVersion: selectCommunications(state).loadingDatasetVersion,
    datasetVersion,
    dataset: selectDataset(state, routeProps.match.params.datasetId),

    experimentRuns: selectDatasetVersionExperimentRuns(
      state,
      routeProps.match.params.datasetVersionId
    ),
    loadingDatasetVersionExperimentRuns:
      selectCommunications(state).loadDatasetVersionExperimentRuns[
        routeProps.match.params.datasetVersionId
      ] || initialCommunication,

    deleting: datasetVersion
      ? selectDeletingDatasetVersion(state, datasetVersion.id)
      : initialCommunication,

    workspaceName: selectCurrentWorkspaceNameOrDefault(state),
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

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.datasetVersion>
>;
type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  RouteProps;

class DatasetVersionPage extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.props.loadDatasetVersion(
      this.props.match.params.datasetVersionId,
      this.props.match.params.datasetId
    );
    this.props.loadDatasetVersionExperimentRuns(
      this.props.match.params.datasetVersionId
    );
  }

  public componentDidUpdate(prevProps: AllProps) {
    if (prevProps.deleting.isRequesting && !this.props.deleting.isRequesting) {
      this.props.history.replace(
        routes.datasetVersions.getRedirectPathWithCurrentWorkspace({
          datasetId: this.props.match.params.datasetId,
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

    if (loadingDatasetVersion.error) {
      return handleCustomErrorWithFallback(
        loadingDatasetVersion.error,
        {
          accessDeniedToEntity: () => (
            <NotFoundPage error={loadingDatasetVersion.error} />
          ),
          entityNotFound: () => (
            <NotFoundPage error={loadingDatasetVersion.error} />
          ),
        },

        error => (
          <AuthorizedLayout>
            <PageCommunicationError error={error} />
          </AuthorizedLayout>
        )
      );
    }

    return (
      <DatasetsPagesLayout>
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
                    <this.SummaryMetaRecord label="Delete">
                      <DeleteFAI
                        faiDataTest="delete-dataset-version-button"
                        onDelete={this.deleteDatasetVersion}
                        confirmText={<>Are you sure?</>}
                      />
                    </this.SummaryMetaRecord>
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
                    <span>
                      {getFormattedDateTime(datasetVersion.dateLogged)}
                    </span>
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
      </DatasetsPagesLayout>
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

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DatasetVersionPage);
