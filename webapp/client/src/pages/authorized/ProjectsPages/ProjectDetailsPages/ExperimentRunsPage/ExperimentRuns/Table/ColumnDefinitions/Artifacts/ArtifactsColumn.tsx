import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import Artifacts from 'features/artifactManager/view/Artifacts/Artifacts';
import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import {
  ICommunicationById,
  initialCommunication,
} from 'core/shared/utils/redux/communication';
import {
  deleteExperimentRunArtifact,
  selectDeletingExperimentRunArtifacts,
} from 'store/experimentRuns';
import { IApplicationState } from 'store/store';

import { IRow } from '../types';

interface ILocalProps {
  row: IRow;
}

interface IPropsFromState {
  deletingExperimentRunArtifacts: ICommunicationById;
}

interface IActionProps {
  deleteExperimentRunArtifact: typeof deleteExperimentRunArtifact;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class ArtifactsColumn extends React.PureComponent<AllProps> {
  public render() {
    const {
      experimentRun: { id, artifacts },
      columnContentHeight,
    } = this.props.row;
    const { deleteExperimentRunArtifact } = this.props;

    return (
      <WithCurrentUserActionsAccesses
        entityType="experimentRun"
        entityId={id}
        actions={['update']}
      >
        {({ actionsAccesses }) => (
          <Artifacts
            entityType="experimentRun"
            entitiyId={id}
            artifacts={artifacts}
            maxHeight={columnContentHeight}
            deletingInfo={{
              isCurrentUserCanDeleteArtifact: actionsAccesses.update,
              delete: deleteExperimentRunArtifact,
              getDeleting: this.getDeleting,
            }}
          />
        )}
      </WithCurrentUserActionsAccesses>
    );
  }

  @bind
  private getDeleting(artifactKey: string) {
    return (
      this.props.deletingExperimentRunArtifacts[
        `${this.props.row.experimentRun.id}-${artifactKey}`
      ] || initialCommunication
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  deletingExperimentRunArtifacts: selectDeletingExperimentRunArtifacts(state),
});

const mapDispatchToProps = (dispatch: Dispatch): IActionProps =>
  bindActionCreators(
    {
      deleteExperimentRunArtifact,
    },
    dispatch
  );

const ConnectedArtifactsColumn = connect(
  mapStateToProps,
  mapDispatchToProps
)(ArtifactsColumn);

export default ConnectedArtifactsColumn;
