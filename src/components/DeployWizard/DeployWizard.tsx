import * as React from 'react';
import { bind } from 'decko';

import DeploySettings from './DeploySettings/DeploySettings';
import Deploying from './Deploying/Deploying';
import DeployResult from './DeployResult/DeployResult';

interface ILocalState {
  step: 'setting' | 'deploying' | 'deployed';
  isShown: boolean;
}

class DeployWizard extends React.PureComponent<{}, ILocalState> {
  public state: ILocalState = { step: 'setting', isShown: true };

  public render() {
    const { step, isShown } = this.state;

    if (!isShown) {
      return null;
    }

    switch (step) {
      case 'setting':
        return <DeploySettings onDeploy={this.onDeploy} onClose={this.onClose} />;
      case 'deploying':
        return <Deploying onClose={this.onClose} />;
      case 'deployed':
        return <DeployResult onClose={this.onClose} />;
    }
  }

  @bind
  private onDeploy() {
    this.setState({ step: 'deploying' });
    setTimeout(() => this.setState({ step: 'deployed' }), 600);
  }

  @bind
  private onClose() {
    this.setState({ isShown: false });
  }
}

export default DeployWizard;
