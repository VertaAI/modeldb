import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import Fab from 'components/shared/Fab/Fab';
import { IDeployStatusInfo } from 'models/Deploy';
import { selectDeployStatusInfo, showDeployWizardForModel } from 'store/deploy';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

interface ILocalProps {
  modelId: string;
}

interface IPropsFromState {
  deployStatusInfo: IDeployStatusInfo;
}

export type AllProps = ILocalProps & IPropsFromState & IConnectedReduxProps;

class DeployButton extends React.PureComponent<AllProps> {
  public render() {
    const { deployStatusInfo } = this.props;

    return (
      <div>
        {(() => {
          switch (deployStatusInfo.status) {
            case 'notDeployed':
            case 'unknown': {
              return (
                <Fab
                  theme="blue"
                  icon="upload"
                  onClick={this.onShowDeployWizard}
                >
                  Deploy
                </Fab>
              );
            }
            case 'deploying': {
              return (
                <Fab
                  variant="outlined"
                  theme="green"
                  onClick={this.onShowDeployWizard}
                >
                  Deploying...
                </Fab>
              );
            }
            case 'deployed': {
              return (
                <Fab theme="green" onClick={this.onShowDeployWizard}>
                  Deployed
                </Fab>
              );
            }
          }
        })()}
      </div>
    );
  }

  @bind
  private onShowDeployWizard() {
    this.props.dispatch(showDeployWizardForModel(this.props.modelId));
  }
}

const mapStateToProps = (
  state: IApplicationState,
  ownProps: ILocalProps
): IPropsFromState => {
  return {
    deployStatusInfo: selectDeployStatusInfo(state, ownProps.modelId),
  };
};

export default connect(mapStateToProps)(DeployButton);
