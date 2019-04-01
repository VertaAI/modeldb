import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import Fab from 'components/shared/Fab/Fab';
import { IDeployStatusInfo } from 'models/Deploy';
import {
  selectDeployStatusInfo,
  showDeployManagerForModel,
  loadDeployStatus,
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
  intervalId: number = 0;

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
                  icon="upload"
                  isLoading={deployStatusInfo.status === 'unknown'}
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

  /*
  @bind
  public componentDidMount() {
    this.intervalId = window.setInterval(() => {
      this.props.dispatch(loadDeployStatus(this.props.modelId));
    }, 5000);
  }

  @bind
  public componentWillUnmount() {
    window.clearInterval(this.intervalId);
  }
  */
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
