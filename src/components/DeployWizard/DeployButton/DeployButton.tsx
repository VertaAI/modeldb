import * as React from 'react';
import { bind } from 'decko';

import Fab from 'components/shared/Fab/Fab';

import DeployWizard from '../DeployWizard';

interface IProps {}

interface ILocalState {
  isDeployWizardShown: boolean;
  step: 'setting' | 'deploying' | 'deployed';
}

class DeployButton extends React.PureComponent<IProps, ILocalState> {
  public state: ILocalState = { isDeployWizardShown: false, step: 'setting' };

  public render() {
    const { isDeployWizardShown, step } = this.state;
    return (
      <div>
        <DeployWizard isShown={isDeployWizardShown} step={step} onDeploy={this.onDeploy} onClose={this.onHideDeployWizard} />
        {(() => {
          switch (step) {
            case 'setting': {
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
    this.setState({ isDeployWizardShown: true });
  }

  @bind
  private onHideDeployWizard() {
    this.setState({ isDeployWizardShown: false });
  }

  @bind
  private onDeploy() {
    this.setState({ step: 'deploying' });
    setTimeout(() => this.setState({ step: 'deployed' }), 600);
  }
}

export default DeployButton;
