import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import { ICommunication } from 'core/shared/utils/redux/communication';
import {
  selectExperimentRunIdsForDeleting,
  unselectExperimentRunForDeleting,
  deleteExperimentRuns,
  selectCommunications,
  resetExperimentRunsForDeleting,
} from 'store/experimentRuns';
import { IApplicationState } from 'store/store';

import TableBulkDeletionManager from 'core/shared/view/domain/BulkDeletion/TableBulkDeletionComponents/TableBulkDeletionManager/TableBulkDeletionManager';

interface ILocalProps {
  projectId: string;
}

interface IPropsFromState {
  experimentRunIdsForDeleting: string[];
  deletingExperimentRuns: ICommunication;
}

interface IActionProps {
  unselectExperimentRunForDeleting: typeof unselectExperimentRunForDeleting;
  deleteExperimentRuns: typeof deleteExperimentRuns;
  resetExperimentRunsForDeleting: typeof resetExperimentRunsForDeleting;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

const DeletingExperimentRunsManager = React.memo((props: AllProps) => (
  <TableBulkDeletionManager
    entityName="Experiment Run"
    entityIds={props.experimentRunIdsForDeleting}
    deleteEntities={ids => props.deleteExperimentRuns(props.projectId, ids)}
    deletingEntities={props.deletingExperimentRuns}
    unselectEntityForDeleting={props.unselectExperimentRunForDeleting}
    resetEntities={props.resetExperimentRunsForDeleting}
  />
));

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    experimentRunIdsForDeleting: selectExperimentRunIdsForDeleting(state),
    deletingExperimentRuns: selectCommunications(state).deletingExperimentRuns,
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      unselectExperimentRunForDeleting,
      deleteExperimentRuns,
      resetExperimentRunsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeletingExperimentRunsManager);
