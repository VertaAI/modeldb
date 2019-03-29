import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import Fab from 'components/shared/Fab/Fab';
import { IDeployConfig, IDeployStatusInfo } from 'models/Deploy';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import {
  deploy,
  selectDeployStatusInfo,
  deployWithCheckingStatus,
  checkDeployStatus,
  loadDeployStatus,
  showDeployWizardForModel
} from 'store/deploy';

import DeployWizard from '../DeployWizard';

interface IProps {
  modelId: string;
}

interface IPropsFromState {
  deployStatusInfo: IDeployStatusInfo;
}

interface ILocalState {
  isDeployWizardShown: boolean;
}

export type AllProps = IProps & IPropsFromState & IConnectedReduxProps;

// todo rename. maybe DeployManager?
class DeployManager extends React.PureComponent<AllProps, ILocalState> {
  public state: ILocalState = { isDeployWizardShown: false };

  public render() {
    const { deployStatusInfo } = this.props;
    const { isDeployWizardShown } = this.state;

    return (
      <div>
        {(() => {
          switch (deployStatusInfo.status) {
            case 'notDeployed':
            case 'unknown': {
              return (
                <Fab theme="blue" icon="upload" onClick={this.onShowDeployWizard}>
                  Deploy
                </Fab>
              );
            }
            case 'deploying': {
              return (
                <Fab variant="outlined" theme="green" onClick={this.onShowDeployWizard}>
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

  @bind
  private onHideDeployWizard() {
    this.setState({ isDeployWizardShown: false });
  }

  @bind
  private onDeploy() {
    this.props.dispatch(deployWithCheckingStatus(this.props.modelId));
  }
}

const mapStateToProps = (state: IApplicationState, ownProps: IProps): IPropsFromState => {
  return {
    deployStatusInfo: selectDeployStatusInfo(state, ownProps.modelId)
  };
};

export default connect(mapStateToProps)(DeployManager);
