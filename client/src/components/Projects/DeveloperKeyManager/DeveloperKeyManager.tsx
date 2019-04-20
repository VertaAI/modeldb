import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';

import DeveloperKeyInfo from 'components/DeveloperKeyInfo/DeveloperKeyInfo';
import Icon from 'components/shared/Icon/Icon';
import {
  hideDeveloperKeyInfo,
  selectIsShowDeveloperKeyInfo,
} from 'store/projectsPage';
import { IApplicationState, IConnectedReduxProps } from 'store/store';
import styles from './DeveloperKeyManager.module.css';

import DeveloperKeyHint from './DeveloperKeyHint/DeveloperKeyHint';

interface IPropsFromState {
  isShowDeveloperKeyInfo: boolean;
}

interface IState {
  display: null | 'info' | 'hint';
}

type IProps = IPropsFromState & IConnectedReduxProps;

// todo maybe rename
class DeveloperKeyManager extends React.PureComponent<IProps, IState> {
  public state: IState = {
    display: this.props.isShowDeveloperKeyInfo ? 'info' : null,
  };

  public render() {
    switch (this.state.display) {
      case null:
        return '';
      case 'info':
        return (
          <DeveloperKeyInfo
            close={
              <Icon
                type="close"
                className={styles.developer_key_info_close}
                onClick={this.makeChangeDisplay('hint')}
              />
            }
          />
        );
      case 'hint':
        return <DeveloperKeyHint onHide={this.makeChangeDisplay(null)} />;
    }
  }

  @bind
  private makeChangeDisplay(display: IState['display']) {
    return () => {
      this.setState({ display });
      if (display === 'hint') {
        this.props.dispatch(hideDeveloperKeyInfo());
      }
    };
  }
}

const mapStateProps = (state: IApplicationState): IPropsFromState => {
  return {
    isShowDeveloperKeyInfo: selectIsShowDeveloperKeyInfo(state),
  };
};

export default connect(mapStateProps)(DeveloperKeyManager);
