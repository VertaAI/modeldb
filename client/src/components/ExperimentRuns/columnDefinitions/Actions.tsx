import * as React from 'react';
import { connect } from 'react-redux';

import DeployManager from 'components/DeployWizard/DeployManager/DeployManager';

class Actions extends React.Component<any> {
  public render() {
    return (
      <div>
        <DeployManager modelId={this.props.data.id} />
      </div>
    );
  }
}

export default Actions;
