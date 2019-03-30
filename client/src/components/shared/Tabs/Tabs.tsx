import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import Tab, { ILocalProps as ITabProps } from './Tab/Tab';

import styles from './Tabs.module.css';

interface ILocalProps<T> {
  active: T;
  children: any;
  onSelectTab(type: T): void;
}

const whiteSpace = '\u00A0';

class Tabs<T> extends React.Component<ILocalProps<T>> {
  public static Tab = Tab;

  public render() {
    const { active } = this.props;
    const activeTabProps = this.getActiveTabProps();
    return (
      <div className={styles.tabs}>
        <div className={styles.titles}>
          {this.getTabsProps().map(({ type, title, badge }) => (
            <button
              className={cn(styles.title, {
                [styles.title_active]: type === active,
              })}
              key={String(type)}
              onClick={this.makeOnSelectTab(type)}
            >
              {title}
              {whiteSpace}
              {badge && <div className={styles.title_badge}>{badge}</div>}
            </button>
          ))}
        </div>
        <div
          className={cn(styles.content, {
            [styles.content_centered]: activeTabProps.centered,
          })}
        >
          {activeTabProps.children}
        </div>
      </div>
    );
  }

  @bind
  private getTabsProps(): Array<ITabProps<T>> {
    return React.Children.map(
      this.props.children,
      child => child.props
    ) as Array<ITabProps<T>>;
  }

  @bind
  private getActiveTabProps(): ITabProps<T> {
    const { active, children } = this.props;
    const activeTab = React.Children.toArray(children).find(
      child => child.props.type === active
    )!;
    return activeTab.props;
  }

  @bind
  private makeOnSelectTab(type: T) {
    return () => {
      if (this.props.active !== type) {
        this.props.onSelectTab(type);
      }
    };
  }
}

export default Tabs;
