import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { RouteComponentProps, withRouter } from 'react-router';

import Button from 'shared/view/elements/Button/Button';
import {
  selectComparedEntityIds,
  selectIsEnableEntitiesComparing,
} from '../../../../store';
import { IApplicationState } from 'setup/store/store';
import routes from 'shared/routes';
import { selectCurrentWorkspaceName } from 'features/workspaces';

interface ILocalProps {
  projectId: string;
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
) => {
  return {
    isEnableEntitiesComparing: selectIsEnableEntitiesComparing(
      state,
      localProps.projectId
    ),
    comparedEntityIds: selectComparedEntityIds(state, localProps.projectId),
    workspaceName: selectCurrentWorkspaceName(state),
  };
};

type AllProps = ILocalProps & ReturnType<typeof mapStateToProps> & RouteComponentProps;

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
      projectId: containerId,
      workspaceName,
    } = this.props;
    return !isEnableEntitiesComparing
      ? undefined
      : routes.compareModels.getRedirectPath({
        modelIds: comparedEntityIds as any,
        projectId: containerId,
        workspaceName, 
      });
  }
}

export default withRouter(connect(mapStateToProps)(CompareEntitiesButton));
