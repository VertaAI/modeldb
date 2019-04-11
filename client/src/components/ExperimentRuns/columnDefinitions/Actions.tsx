import * as React from 'react';

import { DeployButton } from 'components/Deploy';

class Actions extends React.Component<any> {
  public render() {
    return (
      <div>
        <DeployButton modelId={this.props.data.id} />
      </div>
    );
  }
}

export default Actions;
