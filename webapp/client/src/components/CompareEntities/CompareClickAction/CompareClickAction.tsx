import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import {
  selectIsComparedEntity,
  selectIsDisabledSelectionEntitiesForComparing,
  selectEntityForComparing,
  unselectEntityForComparing,
} from 'store/compareEntities';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import FaiLikeCheckbox from 'core/shared/view/elements/FaiLikeCheckbox/FaiLikeCheckbox';

interface ILocalProps {
  enitityId: string;
  containerId: string;
  onHover?(): void;
  onUnhover?(): void;
}

interface IPropsFromState {
  isDisabledSelectionEntitiesForComparing: boolean;
  isSelected: boolean;
}

type AllProps = ILocalProps & IPropsFromState & IConnectedReduxProps;

class CompareClickAction extends React.PureComponent<AllProps> {
  public render() {
    const {
      isSelected,
      isDisabledSelectionEntitiesForComparing,
      onHover,
      onUnhover,
    } = this.props;
    return (
      <FaiLikeCheckbox
        value={isSelected}
        labelWhenChecked={'remove'}
        labelWhenUnchecked={'compare'}
        iconType={'compare-arrows'}
        isDisabled={!isSelected && isDisabledSelectionEntitiesForComparing}
        dataTest="comparing-toggler"
        onHover={onHover}
        onUnhover={onUnhover}
        onChange={this.onChange}
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

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    isDisabledSelectionEntitiesForComparing: selectIsDisabledSelectionEntitiesForComparing(
      state,
      localProps.containerId
    ),
    isSelected: selectIsComparedEntity(
      state,
      localProps.containerId,
      localProps.enitityId
    ),
  };
};

export default connect(mapStateToProps)(CompareClickAction);
