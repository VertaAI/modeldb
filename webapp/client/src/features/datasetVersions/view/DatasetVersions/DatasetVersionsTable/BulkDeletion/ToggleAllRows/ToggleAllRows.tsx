import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import ToggleAllRowsForBulkDeletion from 'shared/view/domain/BulkDeletion/TableBulkDeletionComponents/ToggleAllRowsForBulkDeletion/ToggleAllRowsForBulkDeletion';
import {
  selectAllDatasetVersionsForDeleting,
  resetDatasetVersionsForDeleting,
  selectIsSelectedAllDatasetVersionsForDeleting,
} from 'features/datasetVersions';
import { IApplicationState } from 'setup/store/store';

interface IPropsFromState {
  isSelected: boolean;
}

interface IActionProps {
  selectAll: typeof selectAllDatasetVersionsForDeleting;
  reset: typeof resetDatasetVersionsForDeleting;
}

type AllProps = IPropsFromState & IActionProps;

class ToggleAllDatasetVersionsForBulkDeletion extends React.PureComponent<
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
    isSelected: selectIsSelectedAllDatasetVersionsForDeleting(state),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      selectAll: selectAllDatasetVersionsForDeleting,
      reset: resetDatasetVersionsForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ToggleAllDatasetVersionsForBulkDeletion);
