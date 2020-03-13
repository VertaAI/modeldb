import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps } from 'react-router';
import { bindActionCreators, Dispatch } from 'redux';

import DatasetEntityDescriptionManager from 'components/DescriptionManager/DatasetEntityDescriptionManager/DatasetEntityDescriptionManager';
import Attributes from 'components/ModelRecordProps/Attributes/Attributes/Attributes';
import SummaryInfo from 'components/SummaryViewComponents/SummaryInfo/SummaryInfo';
import DatasetEntityTagsManager from 'components/TagsManager/DatasetEntityTagsManager/DatasetEntityTagsManager';
import { initialCommunication } from 'core/shared/utils/redux/communication';
import DeleteFAI from 'core/shared/view/elements/DeleteFAI/DeleteFAI';
import PageCommunicationError from 'core/shared/view/elements/Errors/PageCommunicationError/PageCommunicationError';
import Preloader from 'core/shared/view/elements/Preloader/Preloader';
import routes, { GetRouteParams } from 'routes';
import {
  selectCommunications,
  selectDataset,
  deleteDataset,
  selectLoadingDataset,
} from 'store/datasets';
import { IApplicationState } from 'store/store';

import DatasetDetailsLayout from '../shared/DatasetDetailsLayout/DatasetDetailsLayout';
import styles from './DatasetSummaryPage.module.css';

const mapDispatchToProps = (dispatch: Dispatch) => {
  return bindActionCreators({ deleteDataset }, dispatch);
};

const mapStateToProps = (state: IApplicationState, routesProps: RouteProps) => {
  return {
    dataset: selectDataset(state, routesProps.match.params.datasetId),
    loadingSelectedDataset: selectLoadingDataset(
      state,
      routesProps.match.params.datasetId
    ),
    deletingSelectedDataset:
      selectCommunications(state).deletingDataset[
        routesProps.match.params.datasetId
      ] || initialCommunication,
  };
};

type RouteProps = RouteComponentProps<
  GetRouteParams<typeof routes.datasetSummary>
>;

type AllProps = ReturnType<typeof mapDispatchToProps> &
  ReturnType<typeof mapStateToProps> &
  RouteProps;

class DatasetSummaryPage extends React.PureComponent<AllProps> {
  public render() {
    const {
      dataset,
      deletingSelectedDataset,
      loadingSelectedDataset,
    } = this.props;

    return (
      <DatasetDetailsLayout
        pagesTabsSettings={{
          isDisabled: deletingSelectedDataset.isRequesting,
          rightContent:
            dataset && loadingSelectedDataset.isSuccess ? (
              <DeleteFAI
                confirmText={
                  <>
                    You're about to delete all data associated with this
                    dataset.
                    <br />
                    Are you sure you want to continue?
                  </>
                }
                faiDataTest="delete-dataset-button"
                onDelete={this.delete}
              />
            ) : (
              undefined
            ),
        }}
      >
        <div
          className={cn(styles.root, {
            [styles.deleting]: deletingSelectedDataset.isRequesting,
          })}
        >
          {(() => {
            if (loadingSelectedDataset.isRequesting) {
              return (
                <div className={styles.preloader}>
                  <Preloader variant="dots" />
                </div>
              );
            }
            if (loadingSelectedDataset.error || !dataset) {
              return (
                <PageCommunicationError error={loadingSelectedDataset.error} />
              );
            }
            return (
              <div className={styles.mainInfo}>
                <SummaryInfo
                  generalInfo={{
                    id: dataset.id,
                    descriptionManagerElement: (
                      <DatasetEntityDescriptionManager
                        datasetId={dataset.id}
                        entityId={dataset.id}
                        entityType="dataset"
                        description={dataset.description}
                      />
                    ),
                  }}
                  detailsInfo={{
                    dateCreated: dataset.dateCreated,
                    dateUpdated: dataset.dateUpdated,
                    tagsManagerElement: (
                      <DatasetEntityTagsManager
                        id={dataset.id}
                        datasetId={dataset.id}
                        entityType="dataset"
                        tags={dataset.tags}
                        isDraggableTags={false}
                      />
                    ),
                    additionalBlock:
                      dataset.attributes.length > 0
                        ? {
                            label: 'Attributes',
                            valueElement: (
                              <Attributes attributes={dataset.attributes} />
                            ),
                          }
                        : undefined,
                  }}
                />
              </div>
            );
          })()}
        </div>
      </DatasetDetailsLayout>
    );
  }

  @bind
  private delete() {
    this.props.deleteDataset(
      this.props.dataset!.id,
      this.props.match.params.workspaceName
    );
  }
}

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DatasetSummaryPage);
