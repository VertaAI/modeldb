import * as React from 'react';
import { bind } from 'decko';

import DeploySettings from './DeploySettings/DeploySettings';
import Deploying from './Deploying/Deploying';
import DeployResult from './DeployResult/DeployResult';

interface IProps {
  isShown: boolean;
  step: 'setting' | 'deploying' | 'deployed';
  onClose(): void;
  onDeploy(): void;
}

class DeployWizard extends React.PureComponent<IProps> {
  public render() {
    const { isShown, step, onClose, onDeploy } = this.props;

    if (!isShown) {
      return null;
    }

    switch (step) {
      case 'setting':
        return <DeploySettings onDeploy={onDeploy} onClose={onClose} />;
      case 'deploying':
        return <Deploying onClose={onClose} />;
      case 'deployed':
        return <DeployResult onClose={onClose} />;
    }
  }
}

export default DeployWizard;
