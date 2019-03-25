import * as React from 'react';

import DeploySettings from './DeploySettings/DeploySettings';
import DeploymentResult from './DeploymentResult/DeploymentResult';
import styles from './DeployWizard.module.css';

class DeployWizard extends React.PureComponent {
  public render() {
    return <DeploySettings />;
  }
}

export default DeployWizard;
