import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import Fab from 'components/shared/Fab/Fab';
import { IDeployInfo, IDeployConfig } from 'models/Deploy';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import { selectDeployInfo, deploy } from 'store/deploy';

import DeployWizard from '../DeployWizard';

interface IProps {
  modelId: string;
}

interface IPropsFromState {
  deployInfo: IDeployInfo;
}

interface ILocalState {
  isDeployWizardShown: boolean;
}

export type AllProps = IProps & IPropsFromState & IConnectedReduxProps;

// todo rename. maybe DeployManager?
class DeployManager extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = { isDeployWizardShown: false };

  public render() {
    const { deployInfo } = this.props;
    const { isDeployWizardShown } = this.state;

    return (
      <div>
        <DeployWizard isShown={isDeployWizardShown} deployInfo={deployInfo} onDeploy={this.onDeploy} onClose={this.onHideDeployWizard} />
        {(() => {
          switch (deployInfo.status) {
            case 'not-deployed': {
              return (
                <Fab theme="blue" icon="upload" onClick={this.onShowDeployWizard}>
                  Deploy
                </Fab>
              );
            }
            case 'building': {
              return (
                <Fab variant="outlined" theme="green" onClick={this.onShowDeployWizard}>
                  Deploying...
                </Fab>
              );
            }
            case 'running': {
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
    this.setState({ isDeployWizardShown: true });
  }

  @bind
  private onHideDeployWizard() {
    this.setState({ isDeployWizardShown: false });
  }

  @bind
  private onDeploy(config: IDeployConfig) {
    this.props.dispatch(deploy(this.props.modelId, config));
  }
}

const mapStateToProps = (state: IApplicationState, ownProps: IProps): IPropsFromState => {
  return {
    deployInfo: selectDeployInfo(state, ownProps.modelId)
  };
};

export default connect(mapStateToProps)(DeployManager);
