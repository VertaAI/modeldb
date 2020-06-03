import * as React from 'react';
import { connect } from 'react-redux';
import { bindActionCreators, Dispatch } from 'redux';

import WidgetBulkDeletion from 'core/shared/view/domain/BulkDeletion/WidgetsBulkDeletionComponents/WidgetBulkDeletion/WidgetBulkDeletion';
import { ICommunication } from 'core/shared/utils/redux/communication';
import {
  selectExperimentIdsForDeleting,
  selectExperimentForDeleting,
  unselectExperimentForDeleting,
  selectDeletingExperiment,
} from 'store/experiments';
import { IApplicationState } from 'store/store';

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
  select: typeof selectExperimentForDeleting;
  unselect: typeof unselectExperimentForDeleting;
}

type AllProps = IPropsFromState & IActionProps & ILocalProps;

const SelectExperimentForDeleting = React.memo((props: AllProps) => (
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
    deleting: selectDeletingExperiment(state, localProps.id),
    isSelected: selectExperimentIdsForDeleting(state).includes(localProps.id),
  };
};

const mapDispatchToProps = (dispatch: Dispatch): IActionProps => {
  return bindActionCreators(
    {
      select: selectExperimentForDeleting,
      unselect: unselectExperimentForDeleting,
    },
    dispatch
  );
};

export default connect(
  mapStateToProps,
  mapDispatchToProps
)(SelectExperimentForDeleting);
