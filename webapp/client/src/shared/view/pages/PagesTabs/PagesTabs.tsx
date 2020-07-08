import cn from 'classnames';
import * as React from 'react';
import { NavLink, withRouter, RouteComponentProps } from 'react-router-dom';

import styles from './PagesTabs.module.css';

interface ILocalProps {
  tabs: ITab[];
  isDisabled?: boolean;
  rightContent?: React.ReactNode;
}

interface ITab {
  label: string;
  to: string;
  or?: string;
}

type AllProps = ILocalProps & RouteComponentProps<{}>;

class PagesTabs extends React.Component<AllProps> {
  public render() {
    const { tabs, isDisabled = false, rightContent } = this.props;

    return (
      <div className={cn(styles.root, { [styles.disabled]: isDisabled })}>
        <nav className={styles.tabs}>
          {tabs.map(tab => this.renderTab(tab))}
        </nav>
        {rightContent && (
          <div className={styles.rightContent}>{rightContent}</div>
        )}
      </div>
    );
  }

  private renderTab({ label, to, or }: ITab) {
    const { isDisabled } = this.props;
    const handleClick = (e: any) =>
      isDisabled ? e.preventDefault() : undefined;
    return (
      <NavLink
        key={to}
        activeClassName={styles.active}
        to={to}
        isActive={
          or ? (_, { pathname }) => [to, or].includes(pathname) : undefined
        }
        onClick={handleClick}
      >
        {label}
      </NavLink>
    );
  }
}

export type IPagesTabsLocalProps = ILocalProps;
export default withRouter(PagesTabs);
