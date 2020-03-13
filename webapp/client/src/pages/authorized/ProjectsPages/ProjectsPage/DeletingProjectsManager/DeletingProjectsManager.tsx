import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import BulkDeletionManager from 'core/shared/view/domain/BulkDeletion/WidgetsBulkDeletionComponents/BulkDeletionManager/BulkDeletionManager';
import { ICommunication } from 'core/shared/utils/redux/communication';
import { IWorkspace } from 'models/Workspace';
import {
  selectProjectIdsForDeleting,
  unselectProjectForDeleting,
  deleteProjects,
  selectCommunications,
  resetProjectsForDeleting,
} from 'store/projects';
import { IApplicationState } from 'store/store';

interface ILocalProps {
  workspaceName: IWorkspace['name'];
}

interface IPropsFromState {
  experimentIdsForDeleting: string[];
  deletingProjects: ICommunication;
}

interface IActionProps {
  unselectProjectForDeleting: typeof unselectProjectForDeleting;
  deleteProjects: typeof deleteProjects;
  resetProjectsForDeleting: typeof resetProjectsForDeleting;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class DeletingProjectsManager extends React.PureComponent<AllProps> {
  public render() {
    return (
      <BulkDeletionManager
        entityName="Project"
        deleteEntities={this.deleteProjects}
        deletingEntities={this.props.deletingProjects}
        entityIds={this.props.experimentIdsForDeleting}
        unselectEntityForDeleting={this.props.unselectProjectForDeleting}
        resetEntities={this.props.resetProjectsForDeleting}
      />
    );
  }

  @bind
  private deleteProjects(ids: string[]) {
    this.props.deleteProjects(ids, this.props.workspaceName);
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    experimentIdsForDeleting: selectProjectIdsForDeleting(state),
    deletingProjects: selectCommunications(state).deletingProjects,
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      unselectProjectForDeleting,
      deleteProjects,
      resetProjectsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(DeletingProjectsManager);
