import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';
import { bindActionCreators, Dispatch } from 'redux';

import { selectCurrentContextFilters } from 'features/filter';
import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { formatBytes } from 'shared/utils/mapperConverters/DataSizeConverted';
import withProps from 'shared/utils/react/withProps';
import Attributes from 'shared/view/domain/ModelRecord/ModelRecordProps/Attributes/Attributes/Attributes';
import WithCurrentUserActionsAccesses from 'shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import DeleteFAIWithLabel from 'shared/view/elements/DeleteFaiWithLabel/DeleteFaiWithLabel';
import PageCommunicationError from 'shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Fai from 'shared/view/elements/Fai/Fai';
import GroupFai from 'shared/view/elements/GroupFai/GroupFai';
import { Icon } from 'shared/view/elements/Icon/Icon';
import IdView from 'shared/view/elements/IdView/IdView';
import Preloader from 'shared/view/elements/Preloader/Preloader';
import { TableWrapper } from 'shared/view/elements/Table/Plugins';
import TablePagingPanel from 'shared/view/elements/Table/Plugins/PagingPanel/TablePagingPanel';
import Table from 'shared/view/elements/Table/Table';
import {
  changeDatasetVersionsPaginationWithLoading,
  deleteDatasetVersion,
  getDefaultDatasetVersionsOptions,
  selectCommunications,
  selectDatasetVersions,
  selectDatasetVersionsPagination,
} from '../../store';
import DatasetEntityTagsManager from 'features/tagsManager/view/DatasetEntityTagsManager/DatasetEntityTagsManager';
import { IDatasetVersion } from 'shared/models/DatasetVersion';
import routes from 'shared/routes';
import { IApplicationState } from 'setup/store/store';

import styles from './DatasetVersions.module.css';
import DeletingDatasetVersionsManager from './DatasetVersionsTable/BulkDeletion/Manager/Manager';
import ToggleAllDatasetVersionsForBulkDeletion from './DatasetVersionsTable/BulkDeletion/ToggleAllRows/ToggleAllRows';
import ToggleDatasetVersionForBulkDeletion from './DatasetVersionsTable/BulkDeletion/ToggleRow/ToggleRow';
import ComparedEntitesManager from 'features/compareDatasets/view/ComparedEntitesManager/ComparedEntitesManager';
import CompareClickAction from 'features/compareDatasets/view/CompareClickAction/CompareClickAction';

const mapStateToProps = (state: IApplicationState) => {
  return {
    pagination: selectDatasetVersionsPagination(state),
    datasetVersions: selectDatasetVersions(state),
    loadingDatasetVersions: selectCommunications(state).loadingDatasetVersions,
    filters: selectCurrentContextFilters(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators(
    {
      deleteDatasetVersion,
      changeDatasetVersionsPaginationWithLoading,
      getDefaultDatasetVersionsOptions,
    },
    dispatch
  );
};

interface ILocalProps {
  datasetId: string;
}

type AllProps = ReturnType<typeof mapStateToProps> &
  ReturnType<typeof mapDispatchToProps> &
  ILocalProps;

interface ILocalState {
  isShowBulkDeletionMenu: boolean;
}

class DatasetVersions extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = {
    isShowBulkDeletionMenu: false,
  };

  public render() {
    const {
      loadingDatasetVersions,
      datasetVersions,
      pagination,
      datasetId,
    } = this.props;
    const { isShowBulkDeletionMenu } = this.state;

    return (
      <div className={styles.root}>
        {(() => {
          if (loadingDatasetVersions.isRequesting) {
            return (
              <div className={styles.preloader}>
                <Preloader variant="dots" />
              </div>
            );
          }
          if (loadingDatasetVersions.error || !datasetVersions) {
            return (
              <PageCommunicationError
                error={loadingDatasetVersions.error}
                isNillEntity={!datasetVersions}
              />
            );
          }
          return (
            <WithCurrentUserActionsAccesses
              entityType={
                datasetVersions.length > 0 ? 'datasetVersion' : 'dataset'
              }
              entityId={
                datasetVersions.length > 0 ? datasetVersions[0].id : datasetId
              }
              actions={['delete']}
            >
              {({ actionsAccesses }) => {
                const withBulkDeletion =
                  actionsAccesses.delete && datasetVersions.length > 0;

                return (
                  <>
                    <div className={styles.dashboard_actions}>
                      <div className={styles.compared_models_manager}>
                        <ComparedEntitesManager
                          containerId={datasetId}
                          getCompareUrl={([
                            datasetVersionId1,
                            datasetVersionId2,
                          ]) =>
                            routes.compareDatasetVersions.getRedirectPathWithCurrentWorkspace(
                              {
                                datasetId,
                                datasetVersionId1,
                                datasetVersionId2,
                              }
                            )
                          }
                        />
                      </div>
                      {actionsAccesses.delete && datasetVersions.length > 0 && (
                        <div className={styles.action_container}>
                          <div className={styles.dashboard_action_label}>
                            Bulk actions:
                          </div>
                          <Fai
                            theme="primary"
                            variant="outlined"
                            icon={<Icon type="list" />}
                            onClick={this.toggleShowingBulkDeletionMenu}
                          />
                        </div>
                      )}
                    </div>
                    <div className={styles.table}>
                      <TableWrapper>
                        <Table
                          dataRows={datasetVersions}
                          getRowKey={this.getRowKey}
                          selection={{
                            headerCellComponent: () => (
                              <ToggleAllDatasetVersionsForBulkDeletion />
                            ),
                            cellComponent: (row) => {
                              return (
                                <ToggleDatasetVersionForBulkDeletion
                                  id={row.id}
                                />
                              );
                            },
                            showSelectAll:
                              withBulkDeletion && isShowBulkDeletionMenu,
                            showSelectionColumn:
                              withBulkDeletion && isShowBulkDeletionMenu,
                          }}
                          columnDefinitions={[
                            {
                              type: 'actions',
                              title: 'Actions',
                              width: '10%',
                              render: (datasetVersion) => {
                                return (
                                  <div data-test="dataset-version">
                                    <GroupFai
                                      groupFai={[
                                        (requiredProps) => (
                                          <DeleteFAIWithLabel
                                            theme="blue"
                                            confirmText="Are you sure?"
                                            dataTest="delete-dataset-version-button"
                                            onDelete={this.makeDeleteDatasetVersion(
                                              datasetVersion.id
                                            )}
                                            {...requiredProps}
                                          />
                                        ),
                                        (requiredProps) => (
                                          <CompareClickAction
                                            containerId={datasetId}
                                            enitityId={datasetVersion.id}
                                            {...requiredProps}
                                          />
                                        ),
                                      ]}
                                    />
                                  </div>
                                );
                              },
                            },
                            {
                              type: 'summary',
                              title: 'Summary',
                              width: '30%',
                              render: (datasetVersion) => {
                                return (
                                  <div className={styles.column_ids}>
                                    <Parameter
                                      label={'id'}
                                      value={
                                        <Link
                                          className={styles.dataset_id}
                                          to={routes.datasetVersion.getRedirectPathWithCurrentWorkspace(
                                            {
                                              datasetId:
                                                datasetVersion.datasetId,
                                              datasetVersionId:
                                                datasetVersion.id,
                                            }
                                          )}
                                        >
                                          <IdView
                                            value={datasetVersion.id}
                                            dataTest={`dataset-version-id-${datasetVersion.id}`}
                                          />
                                        </Link>
                                      }
                                    />
                                    <Parameter
                                      label={'version'}
                                      value={
                                        datasetVersion.version
                                          ? `Version ${datasetVersion.version}`
                                          : 'Version 1'
                                      }
                                      dataTest="dataset-version-name"
                                    />
                                    <Parameter
                                      label={'type'}
                                      value={
                                        <span
                                          className={styles.dataversion_type}
                                        >
                                          {datasetVersion.type}
                                        </span>
                                      }
                                    />
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
                                );
                              },
                            },
                            {
                              type: 'info',
                              title: 'Info',
                              width: '30%',
                              render: (datasetVersion) => {
                                return (
                                  <div className={styles.column_info}>
                                    {datasetVersion.type === 'path' && (
                                      <>
                                        <ParameterMonospace
                                          label={'base path'}
                                          value={
                                            datasetVersion.info.locationType ===
                                            's3FileSystem'
                                              ? safeMap(
                                                  datasetVersion.info.basePath,
                                                  (v) => 's3://' + v
                                                )
                                              : safeMap(
                                                  datasetVersion.info.basePath,
                                                  (v) => v
                                                )
                                          }
                                        />
                                        <Parameter
                                          label={'size'}
                                          value={safeMap(
                                            datasetVersion.info.size,
                                            formatBytes
                                          )}
                                        />
                                        <Parameter
                                          label={'Date Logged'}
                                          value={
                                            datasetVersion.dateLogged ? (
                                              <span>
                                                {getFormattedDateTime(
                                                  datasetVersion.dateLogged
                                                )}
                                              </span>
                                            ) : (
                                              '-'
                                            )
                                          }
                                        />
                                        <Parameter
                                          label={'Date Updated'}
                                          value={
                                            datasetVersion.dateUpdated ? (
                                              <span>
                                                {getFormattedDateTime(
                                                  datasetVersion.dateUpdated
                                                )}
                                              </span>
                                            ) : (
                                              '-'
                                            )
                                          }
                                        />
                                      </>
                                    )}
                                    {datasetVersion.type === 'raw' && (
                                      <>
                                        <Parameter
                                          label={'record count'}
                                          value={datasetVersion.info.numRecords}
                                        />
                                        <Parameter
                                          label={'size'}
                                          value={safeMap(
                                            datasetVersion.info.size,
                                            formatBytes
                                          )}
                                        />
                                        <Parameter
                                          label={'Date Logged'}
                                          value={
                                            datasetVersion.dateLogged ? (
                                              <span>
                                                {getFormattedDateTime(
                                                  datasetVersion.dateLogged
                                                )}
                                              </span>
                                            ) : (
                                              '-'
                                            )
                                          }
                                        />
                                        <Parameter
                                          label={'Date Updated'}
                                          value={
                                            datasetVersion.dateUpdated ? (
                                              <span>
                                                {getFormattedDateTime(
                                                  datasetVersion.dateUpdated
                                                )}
                                              </span>
                                            ) : (
                                              '-'
                                            )
                                          }
                                        />
                                      </>
                                    )}
                                    {datasetVersion.type === 'query' && (
                                      <>
                                        <Parameter
                                          label={'Date Logged'}
                                          value={
                                            datasetVersion.dateLogged ? (
                                              <span>
                                                {getFormattedDateTime(
                                                  datasetVersion.dateLogged
                                                )}
                                              </span>
                                            ) : (
                                              '-'
                                            )
                                          }
                                        />
                                        <Parameter
                                          label={'Date Updated'}
                                          value={
                                            datasetVersion.dateUpdated ? (
                                              <span>
                                                {getFormattedDateTime(
                                                  datasetVersion.dateUpdated
                                                )}
                                              </span>
                                            ) : (
                                              '-'
                                            )
                                          }
                                        />
                                      </>
                                    )}
                                  </div>
                                );
                              },
                            },
                            {
                              type: 'attributes',
                              title: 'Attributes',
                              width: '30%',
                              render: (datasetVersion) => {
                                return (
                                  <Parameter
                                    label={'attributes'}
                                    value={
                                      datasetVersion.attributes.length !== 0 ? (
                                        <Attributes
                                          attributes={datasetVersion.attributes}
                                        />
                                      ) : (
                                        '-'
                                      )
                                    }
                                  />
                                );
                              },
                            },
                          ]}
                        />

                        <div className={styles.footer}>
                          {withBulkDeletion && isShowBulkDeletionMenu && (
                            <div>
                              <DeletingDatasetVersionsManager
                                datasetId={datasetId}
                              />
                            </div>
                          )}
                          <div className={styles.footer__pagination}>
                            <TablePagingPanel
                              pagination={pagination}
                              onCurrentPageChange={
                                this.onPaginationCurrentPageChange
                              }
                            />
                          </div>
                        </div>
                      </TableWrapper>
                    </div>
                  </>
                );
              }}
            </WithCurrentUserActionsAccesses>
          );
        })()}
      </div>
    );
  }

  @bind
  private getRowKey(row: IDatasetVersion) {
    return row.id;
  }

  @bind
  private onPaginationCurrentPageChange(currentPage: number) {
    this.props.changeDatasetVersionsPaginationWithLoading(
      this.props.datasetId,
      currentPage,
      this.props.filters
    );
  }

  @bind
  private makeDeleteDatasetVersion(id: string) {
    return () => this.props.deleteDatasetVersion(this.props.datasetId, id);
  }

  @bind
  private toggleShowingBulkDeletionMenu() {
    this.setState((prev) => ({
      isShowBulkDeletionMenu: !prev.isShowBulkDeletionMenu,
    }));
  }
}

function safeMap<T, B>(
  val: T | string | null | undefined,
  f: (val: T | string) => B
): B | string {
  return val === null || val === undefined || val === '' ? '-' : f(val);
}

const BaseParameter = ({
  label,
  value,
  dataTest,
  renderValue,
}: {
  label: string;
  value: any;
  dataTest?: string;
  renderValue(value: any): React.ReactElement;
}) => {
  return (
    <div className={styles.parameter}>
      <div className={styles.parameter_label}>{label}:</div>
      <div
        className={styles.parameter_value}
        title={typeof value === 'string' ? value : undefined}
        data-test={dataTest}
      >
        {value ? renderValue(value) : '-'}
      </div>
    </div>
  );
};

const ParameterMonospace = withProps(BaseParameter)({
  renderValue: (value) => (
    <span className={cn(styles.monospace_text, styles.monospace_param_value)}>
      {value}
    </span>
  ),
});

const Parameter = withProps(BaseParameter)({ renderValue: (x) => x });

export default connect(mapStateToProps, mapDispatchToProps)(DatasetVersions);
