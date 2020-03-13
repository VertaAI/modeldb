import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import Button from 'core/shared/view/elements/Button/Button';
import {
  ComparedEntityIds,
  selectComparedEntityIds,
  selectIsEnableEntitiesComparing,
} from 'store/compareEntities';
import { IApplicationState } from 'store/store';

interface ILocalProps {
  containerId: string;
  getCompareUrl(comparedEntityIds: Required<ComparedEntityIds>): string;
}

interface IPropsFromState {
  comparedEntityIds: ComparedEntityIds;
  isEnableEntitiesComparing: boolean;
}

type AllProps = ILocalProps & IPropsFromState & RouteComponentProps;

class CompareEntitiesButton extends React.PureComponent<AllProps> {
  public render() {
    const { isEnableEntitiesComparing } = this.props;
    return (
      <Button
        theme="primary"
        disabled={!isEnableEntitiesComparing}
        to={this.getCompareEntitiesLink()}
        dataTest="compare-entities-button"
      >
        Compare
      </Button>
    );
  }

  @bind
  private getCompareEntitiesLink() {
    const {
      isEnableEntitiesComparing,
      comparedEntityIds,
      getCompareUrl,
    } = this.props;
    return !isEnableEntitiesComparing
      ? undefined
      : getCompareUrl(comparedEntityIds as Required<ComparedEntityIds>);
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    isEnableEntitiesComparing: selectIsEnableEntitiesComparing(
      state,
      localProps.containerId
    ),
    comparedEntityIds: selectComparedEntityIds(state, localProps.containerId),
  };
};

export default withRouter(connect(mapStateToProps)(CompareEntitiesButton));
