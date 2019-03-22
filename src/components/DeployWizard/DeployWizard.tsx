import * as React from 'react';

import DeploySettings from './DeploySettings/DeploySettings';
import styles from './DeployWizard.module.css';

class DeployWizard extends React.PureComponent {
  public render() {
    return <DeploySettings />;
  }
}

export default DeployWizard;
