import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import ToggleRowForBulkDeletion from 'core/shared/view/domain/BulkDeletion/TableBulkDeletionComponents/ToggleRowForBulkDeletion/ToggleRowForBulkDeletion';
import {
  selectDatasetVersionForDeleting,
  unselectDatasetVersionForDeleting,
  selectDatasetVersionIdsForDeleting,
} from 'store/datasetVersions';
import { IApplicationState } from 'store/store';

interface ILocalProps {
  id: string;
}

interface IPropsFromState {
  isSelected: boolean;
}

interface IActionProps {
  select: typeof selectDatasetVersionForDeleting;
  unselect: typeof unselectDatasetVersionForDeleting;
}

type AllProps = ILocalProps & IPropsFromState & IActionProps;

class ToggleDatasetVersionForBulkDeletion extends React.PureComponent<
  AllProps
> {
  public render() {
    return (
      <ToggleRowForBulkDeletion
        id={this.props.id}
        isSelected={this.props.isSelected}
        selectEntity={this.props.select}
        unselectEntity={this.props.unselect}
      />
    );
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    isSelected: selectDatasetVersionIdsForDeleting(state).includes(
      localProps.id
    ),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      select: selectDatasetVersionForDeleting,
      unselect: unselectDatasetVersionForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(ToggleDatasetVersionForBulkDeletion);
