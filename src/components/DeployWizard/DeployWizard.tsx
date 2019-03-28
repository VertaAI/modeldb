import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import { IDeployConfig, IDeployStatusInfo } from 'models/Deploy';

import Deploying from './Deploying/Deploying';
import DeployResult from './DeployResult/DeployResult';
import DeploySettings from './DeploySettings/DeploySettings';

interface IProps {
  isShown: boolean;
  deployStatusInfo: IDeployStatusInfo;
  onClose(): void;
  onDeploy(config: IDeployConfig): void;
}

class DeployWizard extends React.PureComponent<IProps> {
  public render() {
    const { isShown, deployStatusInfo, onClose, onDeploy } = this.props;

    if (!isShown) {
      return null;
    }

    switch (deployStatusInfo.status) {
      case 'notDeployed':
        return <DeploySettings status={deployStatusInfo.status} onDeploy={onDeploy} onClose={onClose} />;
      case 'deploying':
        return <Deploying onClose={onClose} />;
      case 'deployed':
        return <DeployResult data={deployStatusInfo.data} onClose={onClose} />;
    }
  }
}

export default DeployWizard;
