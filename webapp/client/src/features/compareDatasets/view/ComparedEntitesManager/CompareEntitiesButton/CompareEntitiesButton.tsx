import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import Button from 'shared/view/elements/Button/Button';
import {
  ComparedDatasetVersionIds,
  selectComparedEntityIds,
  selectIsEnableEntitiesComparing,
} from '../../../store';
import { IApplicationState } from 'setup/store/store';

interface ILocalProps {
  containerId: string;
  getCompareUrl(comparedEntityIds: Required<ComparedDatasetVersionIds>): string;
}

interface IPropsFromState {
  comparedEntityIds: ComparedDatasetVersionIds;
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
      : getCompareUrl(comparedEntityIds as Required<ComparedDatasetVersionIds>);
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
