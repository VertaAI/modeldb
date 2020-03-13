import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import ToggleAllRowsForBulkDeletion from 'core/shared/view/domain/BulkDeletion/TableBulkDeletionComponents/ToggleAllRowsForBulkDeletion/ToggleAllRowsForBulkDeletion';
import {
  selectAllExperimentRunsForDeleting,
  resetExperimentRunsForDeleting,
  selectIsSelectedAllExperimentRunsForDeleting,
} from 'store/experimentRuns';
import { IApplicationState } from 'store/store';

interface IPropsFromState {
  isSelected: boolean;
}

interface IActionProps {
  selectAll: typeof selectAllExperimentRunsForDeleting;
  reset: typeof resetExperimentRunsForDeleting;
}

type AllProps = IPropsFromState & IActionProps;

class ToggleAllExperimentRunsForBulkDeletion extends React.PureComponent<
  AllProps
> {
  public render() {
    return (
      <ToggleAllRowsForBulkDeletion
        isSelected={this.props.isSelected}
        selectAllEntities={this.props.selectAll}
        resetEntities={this.props.reset}
      />
    );
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  return {
    isSelected: selectIsSelectedAllExperimentRunsForDeleting(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      selectAll: selectAllExperimentRunsForDeleting,
      reset: resetExperimentRunsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ToggleAllExperimentRunsForBulkDeletion);
