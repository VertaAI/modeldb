import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import Fab from 'components/shared/Fab/Fab';
import Icon from 'components/shared/Icon/Icon';
import { IDeployStatusInfo } from 'models/Deploy';
import {
  selectDeployStatusInfo,
  showDeployManagerForModel,
} from 'store/deploy';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

interface ILocalProps {
  modelId: string;
}

interface IPropsFromState {
  deployStatusInfo: IDeployStatusInfo;
}

type AllProps = ILocalProps & IPropsFromState & IConnectedReduxProps;

class DeployButton extends React.PureComponent<AllProps> {
  public render() {
    const { deployStatusInfo } = this.props;

    return (
      <div>
        {(() => {
          switch (deployStatusInfo.status) {
            case 'unknown':
            case 'notDeployed': {
              return (
                <Fab
                  theme="blue"
                  icon={<Icon type="upload" />}
                  // isLoading={deployStatusInfo.status === 'unknown'}
                  disabled={deployStatusInfo.status === 'unknown'}
                  onClick={this.onShowDeployManager}
                >
                  Deploy
                </Fab>
              );
            }
            case 'deploying': {
              return (
                <Fab
                  variant="outlined"
                  theme="green"
                  onClick={this.onShowDeployManager}
                >
                  Deploying...
                </Fab>
              );
            }
            case 'deployed': {
              return (
                <Fab theme="green" onClick={this.onShowDeployManager}>
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
  private onShowDeployManager() {
    this.props.dispatch(showDeployManagerForModel(this.props.modelId));
  }
}

const mapStateToProps = (
  state: IApplicationState,
  localProps: ILocalProps
): IPropsFromState => {
  return {
    deployStatusInfo: selectDeployStatusInfo(state, localProps.modelId),
  };
};

export default connect(mapStateToProps)(DeployButton);
