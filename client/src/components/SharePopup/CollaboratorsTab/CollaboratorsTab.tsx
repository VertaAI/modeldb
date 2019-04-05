import { bind } from 'decko';
import React from 'react';
import Scrollbars, { positionValues } from 'react-custom-scrollbars';
import { connect } from 'react-redux';

import Button from 'components/shared/Button/Button';
import ButtonLikeText from 'components/shared/ButtonLikeText/ButtonLikeText';
import { UserAccess } from 'models/Project';
import User from 'models/User';
import { changeProjectOwner, selectAnyError } from 'store/collaboration';
import { IApplicationState, IConnectedReduxProps } from 'store/store';

import CollaboratorItem from '../CollaboratorItem/CollaboratorItem';
import error_icon from '../images/error-icon.svg';
import { PlaceholderInput } from '../PlaceholderInput/PlaceholderInput';
import styles from './CollaboratorsTab.module.css';

interface ILocalProps {
  collaborators: Map<User, UserAccess>;
  currentUserAccess: UserAccess;
  projectId: string;
}

interface IPropsFromState {
  error?: any;
}

interface ILocalState {
  changeOwnerMode: boolean;
  emailValue: string;
  shadowTopOpacity: number;
  shadowBottomOpacity: number;
}

type AllProps = IConnectedReduxProps & ILocalProps & IPropsFromState;

class CollaboratorsTab extends React.Component<AllProps, ILocalState> {
  public state: ILocalState = {
    changeOwnerMode: false,
    emailValue: '',
    shadowBottomOpacity: 0,
    shadowTopOpacity: 0,
  };

  public render() {
    return this.props.error ? (
      <div className={styles.share_result_content}>
        <img src={error_icon} />
        <span className={styles.share_result_header}>
          There are some errors happened…
        </span>
        <ButtonLikeText onClick={this.refreshPage}>Refresh page</ButtonLikeText>
      </div>
    ) : this.state.changeOwnerMode ? (
      <div className={styles.change_owner_content}>
        <div className={styles.content_header}>Change Project Owner</div>
        <div>
          <PlaceholderInput
            additionalClassName={styles.form_group}
            inputValue={this.state.emailValue}
            onInputChange={this.updateInputValue}
            placeholderValue={'New Owner Email'}
          />
        </div>
        <div>
          <div className={styles.change_owner_button}>
            <Button
              theme="gray"
              fullWidth={true}
              onClick={this.hideChangeOwnerMode}
            >
              Cancel
            </Button>
          </div>
          <div className={styles.change_owner_button}>
            <Button fullWidth={true} onClick={this.changeOwnerOnClick}>
              Confirm
            </Button>
          </div>
        </div>
      </div>
    ) : (
      <div style={{ position: 'relative' }}>
        <Scrollbars
          autoHeightMax={'calc(100vh - 200px)'}
          autoHeight={true}
          style={{ margin: '0 16px 0px 40px', width: 'auto' }}
          onScrollFrame={this.handleScrollbarUpdate}
        >
          {Array.from(this.props.collaborators.entries()).map(
            (value: [User, UserAccess], index: number) => {
              const [user, userAccess] = value;
              return (
                <CollaboratorItem
                  key={index}
                  currentUserAccess={this.props.currentUserAccess}
                  projectId={this.props.projectId}
                  user={user}
                  userAccess={userAccess}
                  onChangeOwner={this.showChangeOwnerMode}
                />
              );
            }
          )}
        </Scrollbars>
        <div
          className={styles.shadowTop}
          style={{
            opacity: this.state.shadowTopOpacity,
            visibility:
              this.state.shadowTopOpacity > 0 ? 'visible' : 'collapse',
          }}
        />
        <div
          className={styles.shadowBottom}
          style={{
            opacity: this.state.shadowBottomOpacity,
            visibility:
              this.state.shadowBottomOpacity > 0 ? 'visible' : 'collapse',
          }}
        />
      </div>
    );
  }

  @bind
  private handleScrollbarUpdate(values: positionValues) {
    const { scrollTop, scrollHeight, clientHeight } = values;
    const shadowTopOpacity1 = (1 / 20) * Math.min(scrollTop, 20);
    const bottomScrollTop = scrollHeight - clientHeight;
    const shadowBottomOpacity1 =
      (1 / 20) * (bottomScrollTop - Math.max(scrollTop, bottomScrollTop - 20));
    this.setState({
      ...this.state,
      shadowBottomOpacity: shadowBottomOpacity1,
      shadowTopOpacity: shadowTopOpacity1,
    });
  }

  @bind
  private showChangeOwnerMode() {
    this.setState({ ...this.state, changeOwnerMode: true });
  }

  @bind
  private hideChangeOwnerMode() {
    this.setState({ ...this.state, changeOwnerMode: false, emailValue: '' });
  }

  @bind
  private updateInputValue(event: React.ChangeEvent<HTMLInputElement>) {
    this.setState({
      emailValue: event.target.value,
    });
    event.preventDefault();
  }

  @bind
  private changeOwnerOnClick() {
    this.props.dispatch(
      changeProjectOwner(this.props.projectId, this.state.emailValue)
    );
    this.hideChangeOwnerMode();
  }

  private refreshPage() {
    location.reload();
  }
}

const mapStateToProps = (state: IApplicationState): IPropsFromState => ({
  error: selectAnyError(state),
});

export default connect(mapStateToProps)(CollaboratorsTab);
