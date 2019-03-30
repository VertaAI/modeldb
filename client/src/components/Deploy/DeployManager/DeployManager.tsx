import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import { IDeployStatusInfo } from 'models/Deploy';
import {
  closeDeployWizardForModel,
  deployWithCheckingStatusUntilDeployed,
  selectDeployStatusInfo,
  selectModelId,
} from 'store/deploy';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import Deploying from './Deploying/Deploying';
import DeployResult from './DeployResult/DeployResult';
import DeploySettings from './DeploySettings/DeploySettings';

interface IPropsFromState {
  modelId: string | null;
  deployStatusInfo: IDeployStatusInfo | null;
}

type AllProps = IPropsFromState & IConnectedReduxProps;

class DeployManager extends React.PureComponent<AllProps> {
  public render() {
    const { modelId, deployStatusInfo } = this.props;

    if (!modelId || !deployStatusInfo) {
      return null;
    }

    switch (deployStatusInfo.status) {
      case 'notDeployed':
        return (
          <DeploySettings
            status={deployStatusInfo.status}
            onDeploy={this.onDeploy}
            onClose={this.onClose}
          />
        );
      case 'deploying':
      case 'unknown':
        return <Deploying onClose={this.onClose} />;
      case 'deployed':
        return (
          <DeployResult
            modelId={modelId}
            data={deployStatusInfo.data}
            onClose={this.onClose}
          />
        );
    }
  }

  @bind
  private onDeploy() {
    this.props.dispatch(
      deployWithCheckingStatusUntilDeployed(this.props.modelId!)
    );
  }

  @bind
  private onClose() {
    this.props.dispatch(closeDeployWizardForModel(this.props.modelId!));
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => {
  const modelId = selectModelId(state);
  return {
    deployStatusInfo: modelId ? selectDeployStatusInfo(state, modelId) : null,
    modelId: selectModelId(state),
  };
};

export default connect(mapStateToProps)(DeployManager);
