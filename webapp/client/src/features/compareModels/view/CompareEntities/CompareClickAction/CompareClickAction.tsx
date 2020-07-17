import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  selectIsComparedEntity,
  selectEntityForComparing,
  unselectEntityForComparing,
} from '../../../store';
import { IApplicationState, IConnectedReduxProps } from 'setup/store/store';

import CompareClickActionView from 'shared/view/domain/CompareEntities/CompareClickAction/CompareClickAction';

interface ILocalProps {
  enitityId: string;
  containerId: string;
  onHover?(): void;
  onUnhover?(): void;
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
) => {
  return {
    isSelected: selectIsComparedEntity(
      state,
      localProps.containerId,
      localProps.enitityId
    ),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps> & IConnectedReduxProps;

class CompareClickAction extends React.PureComponent<AllProps> {
  public render() {
    const {
      isSelected,
      onHover,
      onUnhover,
    } = this.props;
    return (
      <CompareClickActionView
        isSelected={isSelected}
        isDisabled={false}
        onChange={this.onChange}
        onHover={onHover}
        onUnhover={onUnhover}
      />
    );
  }

  @bind
  private onChange(value: boolean) {
    const { containerId: projectId, enitityId: modelId } = this.props;
    if (value) {
      this.props.dispatch(
        selectEntityForComparing({ projectId, modelRecordId: modelId })
      );
    } else {
      this.props.dispatch(
        unselectEntityForComparing({ projectId, modelRecordId: modelId })
      );
    }
  }
}

export default connect(mapStateToProps)(CompareClickAction);
