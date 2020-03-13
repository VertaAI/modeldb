import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import BulkDeletionManager from 'core/shared/view/domain/BulkDeletion/WidgetsBulkDeletionComponents/BulkDeletionManager/BulkDeletionManager';
import { ICommunication } from 'core/shared/utils/redux/communication';
import {
  selectExperimentIdsForDeleting,
  unselectExperimentForDeleting,
  deleteExperiments,
  selectCommunications,
  resetExperimentsForDeleting,
} from 'store/experiments';
import { IApplicationState } from 'store/store';

interface ILocalProps {
  projectId: string;
}

interface IPropsFromState {
  experimentIdsForDeleting: string[];
  deletingExperiments: ICommunication;
}

interface IActionProps {
  unselectExperimentForDeleting: typeof unselectExperimentForDeleting;
  deleteExperiments: typeof deleteExperiments;
  resetExperimentsForDeleting: typeof resetExperimentsForDeleting;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

const DeletingExperimentsManager = React.memo((props: AllProps) => (
  <BulkDeletionManager
    entityName="Experiment"
    deleteEntities={ids => props.deleteExperiments(props.projectId, ids)}
    deletingEntities={props.deletingExperiments}
    entityIds={props.experimentIdsForDeleting}
    unselectEntityForDeleting={props.unselectExperimentForDeleting}
    resetEntities={props.resetExperimentsForDeleting}
  />
));

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    experimentIdsForDeleting: selectExperimentIdsForDeleting(state),
    deletingExperiments: selectCommunications(state).deletingExperiments,
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      unselectExperimentForDeleting,
      deleteExperiments,
      resetExperimentsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeletingExperimentsManager);
