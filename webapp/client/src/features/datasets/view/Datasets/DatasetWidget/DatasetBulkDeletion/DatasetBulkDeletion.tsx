import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import WidgetBulkDeletion from 'shared/view/domain/BulkDeletion/WidgetsBulkDeletionComponents/WidgetBulkDeletion/WidgetBulkDeletion';
import { ICommunication } from 'shared/utils/redux/communication';
import {
  selectDatasetIdsForDeleting,
  selectDatasetForDeleting,
  unselectDatasetForDeleting,
  selectDeletingDataset,
} from 'features/datasets/store';
import { IApplicationState } from 'setup/store/store';

interface ILocalProps {
  id: string;
  children: (togglerElement?: React.ReactElement) => React.ReactNode;
  isEnabled: boolean;
}

interface IPropsFromState {
  deleting: ICommunication;
  isSelected: boolean;
}

interface IActionProps {
  select: typeof selectDatasetForDeleting;
  unselect: typeof unselectDatasetForDeleting;
}

type AllProps = IPropsFromState & IActionProps & ILocalProps;

const SelectModelForDeleting = React.memo((props: AllProps) => (
  <WidgetBulkDeletion
    id={props.id}
    isSelected={props.isSelected}
    isEnabled={props.isEnabled}
    deleting={props.deleting}
    selectEntity={props.select}
    unselectEntity={props.unselect}
  >
    {props.children}
  </WidgetBulkDeletion>
));

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    deleting: selectDeletingDataset(state, localProps.id),
    isSelected: selectDatasetIdsForDeleting(state).includes(localProps.id),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      select: selectDatasetForDeleting,
      unselect: unselectDatasetForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SelectModelForDeleting);
