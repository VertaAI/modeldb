import { ClickAwayListener } from '@material-ui/core';
import cn from 'classnames';
import { bind } from 'decko';
import * as R from 'ramda';
import * as React from 'react';

import Button from '../Button/Button';
import { Icon } from '../Icon/Icon';
import styles from './SplitButton.module.css';

interface ILocalProps {
  theme: 'blue' | 'red';
  disabled?: boolean;
  isLoading?: boolean;
  primaryAction: IAction;
  dataTest?: string;
  otherActions: [IAction, ...IAction[]];
}

interface IAction {
  label: string;
  dataTest?: string;
  onApply(): void;
}

interface ILocalState {
  isShowOtherActions: boolean;
}

const caretDownIcon = <Icon type="caret-down" />;

class SplitButton extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = { isShowOtherActions: false };

  private rootRefObject = React.createRef<HTMLDivElement>();
  private rootNodeInfo: { width?: number; left?: number } = {
    width: undefined,
    left: undefined,
  };

  public componentDidMount() {
    if (this.rootRefObject.current) {
      this.rootNodeInfo = {
        width: this.rootRefObject.current.offsetWidth,
        left: this.rootRefObject.current.getBoundingClientRect().left,
      };
    }
  }

  public render() {
    const {
      primaryAction,
      otherActions,
      isLoading,
      disabled,
      theme,
    } = this.props;
    const { isShowOtherActions } = this.state;
    return (
      <div className={styles.root} ref={this.rootRefObject}>
        <div className={styles.primaryAction}>
          <Button
            theme={theme === 'blue' ? 'primary' : theme}
            dataTest={primaryAction.dataTest}
            isLoading={isLoading}
            disabled={disabled}
            fullWidth={true}
            onClick={this.makeOnActionClick(primaryAction)}
          >
            {primaryAction.label}
          </Button>
        </div>
        <div className={styles.toggler}>
          <Button
            theme={theme === 'blue' ? 'primary' : theme}
            disabled={disabled || isLoading}
            dataTest={this.props.dataTest || 'split-button'}
            fullWidth={true}
            onClick={this.toggleOtherActions}
          >
            {caretDownIcon}
          </Button>
        </div>
        {isShowOtherActions && (
          <ClickAwayListener onClickAway={this.closeOtherActions}>
            <div
              className={cn(styles.otherActions, {
                [styles.opened]: isShowOtherActions,
              })}
            >
              {otherActions.map((action, i) => (
                <div
                  key={i}
                  className={styles.otherAction}
                  onClick={this.makeOnActionClick(action)}
                >
                  {action.label}
                </div>
              ))}
            </div>
          </ClickAwayListener>
        )}
      </div>
    );
  }

  // tslint:disable-next-line: member-ordering
  private makeOnActionClick = R.memoizeWith(
    action => action.label,
    (action: IAction) => {
      return () => {
        action.onApply();
        if (this.state.isShowOtherActions) {
          this.closeOtherActions();
        }
      };
    }
  );

  @bind
  private toggleOtherActions() {
    console.log('toggle');
    if (this.state.isShowOtherActions) {
      this.closeOtherActions();
    } else {
      this.showOtherActions();
    }
  }
  @bind
  private showOtherActions() {
    this.setState({ isShowOtherActions: true });
  }
  @bind
  private closeOtherActions() {
    console.log('close');
    this.setState({ isShowOtherActions: false });
  }
}

export default SplitButton;
