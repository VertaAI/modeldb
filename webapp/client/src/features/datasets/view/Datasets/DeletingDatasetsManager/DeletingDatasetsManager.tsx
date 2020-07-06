import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import BulkDeletionManager from 'shared/view/domain/BulkDeletion/WidgetsBulkDeletionComponents/BulkDeletionManager/BulkDeletionManager';
import { ICommunication } from 'shared/utils/redux/communication';
import { IWorkspace } from 'shared/models/Workspace';
import {
  selectDatasetIdsForDeleting,
  unselectDatasetForDeleting,
  deleteDatasets,
  selectCommunications,
  resetDatasetsForDeleting,
} from 'features/datasets/store';
import { IApplicationState } from 'setup/store/store';

interface ILocalProps {
  workspaceName: IWorkspace['name'];
}

interface IPropsFromState {
  experimentIdsForDeleting: string[];
  deletingDatasets: ICommunication;
}

interface IActionProps {
  unselectDatasetForDeleting: typeof unselectDatasetForDeleting;
  deleteDatasets: typeof deleteDatasets;
  resetDatasetsForDeleting: typeof resetDatasetsForDeleting;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class DeletingDatasetsManager extends React.PureComponent<AllProps> {
  public render() {
    return (
      <BulkDeletionManager
        entityName="Dataset"
        deleteEntities={this.deleteDatasets}
        deletingEntities={this.props.deletingDatasets}
        entityIds={this.props.experimentIdsForDeleting}
        unselectEntityForDeleting={this.props.unselectDatasetForDeleting}
        resetEntities={this.props.resetDatasetsForDeleting}
      />
    );
  }

  @bind
  private deleteDatasets(entityIds: string[]) {
    this.props.deleteDatasets(entityIds, this.props.workspaceName);
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    experimentIdsForDeleting: selectDatasetIdsForDeleting(state),
    deletingDatasets: selectCommunications(state).deletingDatasets,
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      unselectDatasetForDeleting,
      deleteDatasets,
      resetDatasetsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeletingDatasetsManager);
