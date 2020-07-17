import cn from 'classnames';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import routes, { GetRouteParams } from 'shared/routes';
import DataSourceUri from 'shared/view/domain/DatasetVersionProps/QueryDatasetVersionProps/DataSourceUri/DataSourceUri';
import { IKeyValuePair } from 'shared/models/Common';
import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { formatBytes } from 'shared/utils/mapperConverters';
import withProps from 'shared/utils/react/withProps';
import { initialCommunication } from 'shared/utils/redux/communication';
import { matchRemoteData } from 'shared/utils/redux/communication/remoteData';
import removeQuotes from 'shared/utils/removeQuotes';
import InlineCommunicationError from 'shared/view/elements/Errors/InlineCommunicationError/InlineCommunicationError';
import IdView from 'shared/view/elements/IdView/IdView';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import ScrollableContainer from 'shared/view/elements/ScrollableContainer/ScrollableContainer';
import {
  DatasetVersionPathLocationType,
  IPathBasedDatasetVersionInfo,
  IQueryDatasetVersionInfo,
  IRawDatasetVersionInfo,
  IPathBasedDatasetVersion,
} from 'shared/models/DatasetVersion';
import DatasetVersionExperimentRuns from 'features/datasetVersions/view/DatasetVersion/DatasetVersionExperimentRuns/DatasetVersionExperimentRuns';
import { pathLocationLabels } from 'features/datasetVersions/view/shared/constants';
import { selectDataset } from 'features/datasets/store';
import {
  loadComparedDatasetVersions,
  selectCommunications,
  loadDatasetVersionExperimentRuns,
  selectDatasetVersionExperimentRuns,
} from 'features/datasetVersions/store';
import { IApplicationState, IConnectedReduxProps } from 'setup/store/store';
import { selectCurrentWorkspaceName } from 'features/workspaces/store';

import ComparableAttributes from './ComparableAttributes/ComparableAttributes';
import styles from './CompareDatasetVersions.module.css';
import CompareDatasetVersionsTable, {
  IPropDefinitionRenderProps,
  PropDefinition,
} from './CompareDatasetVersionsTable/CompareDatasetVersionsTable';
import ComparePathInfoTable from './ComparePathInfoTable/ComparePathInfoTable';
import Reloading from 'shared/view/elements/Reloading/Reloading';
import { ComparedDatasetVersions, IPathDatasetVersionDifferentProps, DatasetVerisonEntityType } from 'features/compareDatasets/store/compareDatasets';
import { selectDatasetVersionsDifferentProps, selectComparedDatasetVersions } from 'features/compareDatasets/store/selectors';
import { getDiffValueBgClassname } from '../shared/DiffHighlight';
import { ComparedDatasetVersionIds } from 'features/compareDatasets/store';

interface ILocalProps {
  datasetId: string;
  comparedDatasetVersionIds: Required<ComparedDatasetVersionIds>;
}

const mapStateToProps = (state: IApplicationState, localProps: ILocalProps) => {
  return {
    dataset: selectDataset(state, localProps.datasetId)!,
    datasetVersionsDifferentProps: selectDatasetVersionsDifferentProps(
      state,
      localProps.comparedDatasetVersionIds
    ),
    comparedDatasetVersions: selectComparedDatasetVersions(
      state,
      localProps.comparedDatasetVersionIds
    ),
    isLoadingComparedDatasetVersions: selectCommunications(state)
      .loadingComparedDatasetVersions.isRequesting,

    experimentRuns1: selectDatasetVersionExperimentRuns(
      state,
      localProps.comparedDatasetVersionIds[0]!
    ),
    loadingExperimentRuns1:
      selectCommunications(state).loadDatasetVersionExperimentRuns[
      localProps.comparedDatasetVersionIds[0]!
      ] || initialCommunication,
    loadingExperimentRuns2:
      selectCommunications(state).loadDatasetVersionExperimentRuns[
      localProps.comparedDatasetVersionIds[1]!
      ] || initialCommunication,
    experimentRuns2: selectDatasetVersionExperimentRuns(
      state,
      localProps.comparedDatasetVersionIds[1]!
    ),

    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = ILocalProps &
  ReturnType<typeof mapStateToProps> &
  IConnectedReduxProps &
  RouteComponentProps<GetRouteParams<typeof routes.compareDatasetVersions>>;

class CompareDatasetVersions extends React.PureComponent<AllProps> {
  public componentDidMount() {
    this.loadVersionsInfo();
  }

  public render() {
    const {
      comparedDatasetVersions,
      dataset,
      datasetVersionsDifferentProps,
      isLoadingComparedDatasetVersions,
      workspaceName,
    } = this.props;

    return (
      <Reloading onReload={this.loadVersionsInfo}>
        <div className={styles.root}>
          {isLoadingComparedDatasetVersions ||
            comparedDatasetVersions.length !== 2 ||
            !datasetVersionsDifferentProps ||
            !dataset ? (
              <div data-test="compare-dataset-versions-prealoder">
                <Preloader variant="dots" />
              </div>
            ) : (
              <div className={styles.content}>
                <div className={'compare_table'}>
                  <CompareDatasetVersionsTable
                    datasetVersions={
                      comparedDatasetVersions as Required<ComparedDatasetVersions>
                    }
                    datasetVersionsDifferentProps={datasetVersionsDifferentProps}
                    columns={{
                      entity1: { title: 'Version 1' },
                      entity2: { title: 'Version 2' },
                    }}
                  >
                    <PropDefinition
                      prop="id"
                      title="Id"
                      getValue={({ id }) => id}
                      render={withProps(SingleValue)({
                        isId: true,
                        propertyType: 'id',
                      })}
                    />
                    <PropDefinition
                      prop="parentId"
                      title="Parent Id"
                      getValue={({ parentId }) => parentId}
                      render={withProps(SingleValue)({
                        isId: true,
                        propertyType: 'parentId',
                      })}
                    />
                    <PropDefinition
                      prop="version"
                      title="Version"
                      getValue={({ version }) => version}
                      render={withProps(SingleValue)({ propertyType: 'version' })}
                    />
                    <PropDefinition
                      prop="attributes"
                      title="Attributes"
                      getValue={({ attributes }) => attributes}
                      render={({ value, enitityType: datasetVersionType }) =>
                        value.length > 0 ? (
                          <ComparableAttributes
                            entityType={datasetVersionType}
                            entity1Attributes={
                              comparedDatasetVersions[0]!.attributes
                            }
                            entity2Attributes={
                              comparedDatasetVersions[1]!.attributes
                            }
                            diffInfo={datasetVersionsDifferentProps.attributes}
                          />
                        ) : (
                            <span data-test="property-value-attributes">-</span>
                          )
                      }
                    />
                    <PropDefinition
                      prop="dateLogged"
                      title="Timestamp"
                      getValue={({ dateLogged }) => dateLogged}
                      render={props => (
                        <SingleValue
                          {...props}
                          propertyType="dateLogged"
                          value={
                            props.value
                              ? getFormattedDateTime(props.value)
                              : props.value
                          }
                        />
                      )}
                    />
                    {(() => {
                      switch (dataset.type) {
                        case 'raw': {
                          return (
                            <>
                              <PropDefinition
                                prop="numRecords"
                                title="Records count"
                                getValue={({ info }) =>
                                  (info as IRawDatasetVersionInfo).numRecords
                                }
                                render={withProps(SingleValue)({
                                  propertyType: 'numRecords',
                                })}
                              />
                              <PropDefinition
                                prop="size"
                                title="Size"
                                getValue={({ info }) =>
                                  (info as IRawDatasetVersionInfo).size
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    propertyType="size"
                                    value={
                                      !R.isNil(props.value)
                                        ? formatBytes(props.value)
                                        : '-'
                                    }
                                  />
                                )}
                              />
                              <PropDefinition
                                prop="objectPath"
                                title="Object path"
                                getValue={({ info }) =>
                                  (info as IRawDatasetVersionInfo).objectPath
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    propertyType="objectPath"
                                    value={
                                      props.value ? (
                                        <span
                                          className={styles.monospace_text}
                                          title={props.value}
                                        >
                                          {props.value}
                                        </span>
                                      ) : (
                                          undefined
                                        )
                                    }
                                  />
                                )}
                              />
                              <PropDefinition
                                prop="checkSum"
                                title="Checksum"
                                getValue={({ info }) =>
                                  (info as IRawDatasetVersionInfo).checkSum
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    propertyType="checkSum"
                                    value={
                                      props.value ? (
                                        <span
                                          className={styles.monospace_text}
                                          title={removeQuotes(props.value)}
                                        >
                                          {removeQuotes(props.value)}
                                        </span>
                                      ) : (
                                          undefined
                                        )
                                    }
                                  />
                                )}
                              />
                              <PropDefinition
                                prop="features"
                                title="Features"
                                getValue={({ info }) =>
                                  (info as IRawDatasetVersionInfo).features
                                }
                                render={({
                                  value,
                                  enitityType: modelType,
                                  diffInfo: differentValues,
                                }) =>
                                  value.length > 0 ? (
                                    <ScrollableContainer
                                      listContent={value}
                                      maxHeight={150}
                                      containerOffsetValue={20}
                                      getValueAdditionalClassname={val =>
                                        getDiffValueBgClassname(
                                          modelType,
                                          differentValues[val]
                                        )
                                      }
                                    />
                                  ) : (
                                      '-'
                                    )
                                }
                              />
                            </>
                          );
                        }
                        case 'query': {
                          return (
                            <>
                              <PropDefinition
                                prop="numRecords"
                                title="Records count"
                                getValue={({ info }) =>
                                  (info as IQueryDatasetVersionInfo).numRecords
                                }
                                render={withProps(SingleValue)({
                                  propertyType: 'numRecords',
                                })}
                              />
                              <PropDefinition
                                prop="dataSourceUri"
                                title="Data source URL"
                                getValue={({ info }) =>
                                  (info as IQueryDatasetVersionInfo).dataSourceUri
                                }
                                render={props => {
                                  const dataSourceUri = (props.datasetVersion
                                    .info as IQueryDatasetVersionInfo)
                                    .dataSourceUri;
                                  return dataSourceUri ? (
                                    <div
                                      className={styles.truncated_text}
                                      title={dataSourceUri}
                                    >
                                      <DataSourceUri
                                        value={dataSourceUri}
                                        additionalClassname={getDiffValueBgClassname(
                                          props.enitityType,
                                          props.diffInfo
                                        )}
                                      />
                                    </div>
                                  ) : (
                                      '-'
                                    );
                                }}
                              />
                              <PropDefinition
                                prop="query"
                                title="Query"
                                getValue={({ info }) =>
                                  (info as IQueryDatasetVersionInfo).query
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    propertyType="query"
                                    value={
                                      props.value ? (
                                        <span
                                          title={props.value}
                                          className={styles.monospace_text}
                                        >
                                          {props.value}
                                        </span>
                                      ) : (
                                          undefined
                                        )
                                    }
                                  />
                                )}
                              />
                              <PropDefinition
                                prop="queryParameters"
                                title="Query parameters"
                                getValue={({ info }) =>
                                  (info as IQueryDatasetVersionInfo)
                                    .queryParameters
                                }
                                render={({
                                  value,
                                  enitityType: modelType,
                                  diffInfo: differentValues,
                                }) => (
                                    <KeyValuePairs
                                      value={(value as IQueryDatasetVersionInfo['queryParameters']).map(
                                        ({ name, value }) => ({ key: name, value })
                                      )}
                                      getValueClassname={key =>
                                        getDiffValueBgClassname(
                                          modelType,
                                          differentValues[key]
                                        )
                                      }
                                    />
                                  )}
                              />
                              <PropDefinition
                                prop="queryTemplate"
                                title="Query template"
                                getValue={({ info }) =>
                                  (info as IQueryDatasetVersionInfo).queryTemplate
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    value={
                                      props.value ? (
                                        <span
                                          title={props.value}
                                          className={styles.monospace_text}
                                        >
                                          {props.value}
                                        </span>
                                      ) : (
                                          undefined
                                        )
                                    }
                                  />
                                )}
                              />
                            </>
                          );
                        }
                        case 'path': {
                          return (
                            <>
                              <PropDefinition
                                prop="size"
                                title="Size"
                                getValue={({ info }) =>
                                  (info as IPathBasedDatasetVersionInfo).size
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    propertyType="size"
                                    value={
                                      !R.isNil(props.value)
                                        ? formatBytes(props.value)
                                        : undefined
                                    }
                                  />
                                )}
                              />
                              <PropDefinition
                                prop="basePath"
                                title="Base path"
                                getValue={({ info }) =>
                                  (info as IPathBasedDatasetVersionInfo).basePath
                                }
                                render={withProps(SingleValue)({
                                  propertyType: 'basePath',
                                })}
                              />
                              <PropDefinition<IPathBasedDatasetVersion>
                                prop="datasetPathInfos"
                                title="Dataset path info"
                                getValue={({ info }) => info.datasetPathInfos}
                                render={_ => {
                                  const [
                                    datasetVersion1,
                                    datasetVersion2,
                                  ] = comparedDatasetVersions as Required<
                                    ComparedDatasetVersions
                                  >;
                                  const pathInfos1 = (datasetVersion1 as IPathBasedDatasetVersion)
                                    .info.datasetPathInfos;
                                  const pathInfos2 = (datasetVersion2 as IPathBasedDatasetVersion)
                                    .info.datasetPathInfos;
                                  return pathInfos1.length > 0 &&
                                    pathInfos2.length > 0 ? (
                                      <ComparePathInfoTable
                                        diffInfo={
                                          (datasetVersionsDifferentProps as IPathDatasetVersionDifferentProps)
                                            .datasetPathInfos
                                        }
                                        pathInfos1={pathInfos1}
                                        pathInfos2={pathInfos2}
                                      />
                                    ) : (
                                      '-'
                                    );
                                }}
                              />
                              <PropDefinition
                                prop="locationType"
                                title="Location"
                                getValue={({ info }) =>
                                  (info as IPathBasedDatasetVersionInfo)
                                    .locationType
                                }
                                render={props => (
                                  <SingleValue
                                    {...props}
                                    propertyType="locationType"
                                    value={
                                      pathLocationLabels[
                                      props.value as DatasetVersionPathLocationType
                                      ]
                                    }
                                  />
                                )}
                              />
                            </>
                          );
                        }
                      }
                    })()}
                    <PropDefinition
                      title="Experiment runs"
                      prop="experimentRuns"
                      getValue={datasetVersion => datasetVersion}
                      render={props =>
                        matchRemoteData(
                          props.enitityType === DatasetVerisonEntityType.entity1
                            ? this.props.loadingExperimentRuns1
                            : this.props.loadingExperimentRuns2,
                          props.enitityType === DatasetVerisonEntityType.entity1
                            ? this.props.experimentRuns1
                            : this.props.experimentRuns2,
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
                                    isCompacted={true}
                                    workspaceName={workspaceName}
                                  />
                                </div>
                              ) : (
                                  '-'
                                ),
                          }
                        )
                      }
                    />
                  </CompareDatasetVersionsTable>
                </div>
              </div>
            )}
        </div>
      </Reloading>
    );
  }

  @bind
  private loadVersionsInfo() {
    const {
      dispatch,
      datasetId,
      comparedDatasetVersionIds,
      workspaceName,
    } = this.props;
    dispatch(
      loadComparedDatasetVersions(
        workspaceName,
        datasetId,
        comparedDatasetVersionIds[0],
        comparedDatasetVersionIds[1]
      )
    );
    dispatch(
      loadDatasetVersionExperimentRuns(
        workspaceName,
        comparedDatasetVersionIds[0]
      )
    );
    dispatch(
      loadDatasetVersionExperimentRuns(
        workspaceName,
        comparedDatasetVersionIds[1]
      )
    );
  }
}

function SingleValue<Props extends IPropDefinitionRenderProps>({
  value,
  enitityType: modelType,
  diffInfo: isDifferent,
  propertyType,
  isId,
}: Props & {
  diffInfo: boolean;
  value: any;
  isId?: boolean;
  propertyType?: string;
}) {
  return !R.isNil(value) ? (
    <span
      className={getDiffValueBgClassname(modelType, isDifferent)}
      title={
        typeof value === 'string' || typeof value === 'number'
          ? String(value)
          : undefined
      }
      data-test={`property-value-${propertyType}`}
    >
      {isId ? <IdView value={value} /> : value}
    </span>
  ) : (
      <span data-test={`property-value-${propertyType}`}>-</span>
    );
}

function KeyValuePairs({
  value,
  getValueClassname,
}: {
  value: Array<IKeyValuePair<string>>;
  getValueClassname: (key: string) => string | undefined;
}): JSX.Element {
  return value.length > 0 ? (
    <div className={styles.key_value_pairs}>
      {value.map(({ key, value }) => (
        <div className={styles.key_value_pair}>
          <div className={styles.key} title={key}>
            {key}
          </div>
          <div
            className={cn(styles.value, getValueClassname(key))}
            title={value}
          >
            {value}
          </div>
        </div>
      ))}
    </div>
  ) : (
      <span>-</span>
    );
}

export type ICompareDatasetVersionsLocalProps = ILocalProps;
export default connect(mapStateToProps)(withRouter(CompareDatasetVersions));
