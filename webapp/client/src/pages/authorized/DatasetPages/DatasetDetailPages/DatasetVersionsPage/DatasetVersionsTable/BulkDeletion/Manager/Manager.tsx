import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { ICommunication } from 'core/shared/utils/redux/communication';
import {
  selectDatasetVersionIdsForDeleting,
  unselectDatasetVersionForDeleting,
  deleteDatasetVersions,
  selectCommunications,
  resetDatasetVersionsForDeleting,
} from 'store/datasetVersions';
import { IApplicationState } from 'store/store';

import TableBulkDeletionManager from 'core/shared/view/domain/BulkDeletion/TableBulkDeletionComponents/TableBulkDeletionManager/TableBulkDeletionManager';

interface ILocalProps {
  datasetId: string;
}

interface IPropsFromState {
  experimentRunIdsForDeleting: string[];
  deletingDatasetVersions: ICommunication;
}

interface IActionProps {
  unselectDatasetVersionForDeleting: typeof unselectDatasetVersionForDeleting;
  deleteDatasetVersions: typeof deleteDatasetVersions;
  resetDatasetVersionsForDeleting: typeof resetDatasetVersionsForDeleting;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

const DeletingDatasetVersionsManager = React.memo((props: AllProps) => (
  <TableBulkDeletionManager
    entityName="Dataset Version"
    entityIds={props.experimentRunIdsForDeleting}
    deleteEntities={ids => props.deleteDatasetVersions(props.datasetId, ids)}
    deletingEntities={props.deletingDatasetVersions}
    unselectEntityForDeleting={props.unselectDatasetVersionForDeleting}
    resetEntities={props.resetDatasetVersionsForDeleting}
  />
));

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    experimentRunIdsForDeleting: selectDatasetVersionIdsForDeleting(state),
    deletingDatasetVersions: selectCommunications(state)
      .deletingDatasetVersions,
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      unselectDatasetVersionForDeleting,
      deleteDatasetVersions,
      resetDatasetVersionsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeletingDatasetVersionsManager);
