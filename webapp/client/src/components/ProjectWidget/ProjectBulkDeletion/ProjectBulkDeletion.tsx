import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import WidgetBulkDeletion from 'core/shared/view/domain/BulkDeletion/WidgetsBulkDeletionComponents/WidgetBulkDeletion/WidgetBulkDeletion';
import { IApplicationState } from 'store/store';

import { ICommunication } from 'core/shared/utils/redux/communication';
import {
  selectProjectForDeleting,
  unselectProjectForDeleting,
  selectProjectIdsForDeleting,
  selectDeletingProject,
} from 'store/projects';

interface ILocalProps {
  id: string;
  children: (togglerElement?: React.ReactElement) => React.ReactNode;
  isEnabled: boolean;
}

interface IPropsFromState {
  isSelected: boolean;
  deleting: ICommunication;
}

interface IActionProps {
  select: typeof selectProjectForDeleting;
  unselect: typeof unselectProjectForDeleting;
}

type AllProps = IPropsFromState & IActionProps & ILocalProps;

const SelectModelForDeleting = React.memo((props: AllProps) => (
  <WidgetBulkDeletion
    id={props.id}
    selectEntity={props.select}
    unselectEntity={props.unselect}
    isSelected={props.isSelected}
    isEnabled={props.isEnabled}
    deleting={props.deleting}
  >
    {props.children}
  </WidgetBulkDeletion>
));

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    deleting: selectDeletingProject(state, localProps.id),
    isSelected: selectProjectIdsForDeleting(state).includes(localProps.id),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      select: selectProjectForDeleting,
      unselect: unselectProjectForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SelectModelForDeleting);
