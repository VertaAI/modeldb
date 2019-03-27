import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import { IDeployConfig, IDeployResult, IDeployInfo } from 'models/Deploy';

import DeploySettings from './DeploySettings/DeploySettings';
import Deploying from './Deploying/Deploying';
import DeployResult from './DeployResult/DeployResult';

interface IProps {
  isShown: boolean;
  deployInfo: IDeployInfo;
  onClose(): void;
  onDeploy(config: IDeployConfig): void;
}

class DeployWizard extends React.PureComponent<IProps> {
  public render() {
    const { isShown, deployInfo, onClose, onDeploy } = this.props;

    if (!isShown) {
      return null;
    }

    switch (deployInfo.status) {
      case 'not-deployed':
        return <DeploySettings status={deployInfo.status} onDeploy={onDeploy} onClose={onClose} />;
      case 'building':
        return <Deploying onClose={onClose} />;
      case 'running':
        return <DeployResult deployResult={deployInfo.result} onClose={onClose} />;
    }
  }
}

export default DeployWizard;
