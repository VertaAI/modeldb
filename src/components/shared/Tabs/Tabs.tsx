import * as React from 'react';
import { bind } from 'decko';

import Tab, { IProps as ITabProps } from './Tab/Tab';

import styles from './Tabs.module.css';

interface IProps<T> {
  active: T;
  children: any;
  onSelectTab(type: T): void;
}

const whiteSpace = '\u00A0';

class Tabs<T> extends React.Component<IProps<T>> {
  public static Tab: typeof Tab = Tab;

  public render() {
    const { active } = this.props;
    const activeTabProps = this.getActiveTabProps();
    return (
      <div className={styles.tabs}>
        <div className={styles.titles}>
          {this.getTabsProps().map(({ type, title, badge, centered }) => (
            <button
              className={`${styles.title} ${type === active ? styles.title_active : ''}`}
              key={String(type)}
              onClick={this.makeOnSelectTab(type)}
            >
              {title}
              {whiteSpace}
              {badge && <div className={styles.title_badge}>{badge}</div>}
            </button>
          ))}
        </div>
        <div className={`${styles.content} ${activeTabProps.centered ? styles.content_centered : ''}`}>{activeTabProps.children}</div>
      </div>
    );
  }

  @bind
  private getTabsProps(): ITabProps<T>[] {
    return React.Children.map(this.props.children, child => child.props) as ITabProps<T>[];
  }

  @bind
  private getActiveTabProps(): ITabProps<T> {
    const { active, children } = this.props;
    const activeTab = React.Children.toArray(children).find(child => child.props.type === active)!;
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
