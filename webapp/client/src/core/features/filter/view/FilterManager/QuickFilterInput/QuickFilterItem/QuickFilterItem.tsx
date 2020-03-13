import { bind } from 'decko';
import * as React from 'react';

import { IQuickFilter } from 'core/features/filter/Model';
import { Icon } from 'core/shared/view/elements/Icon/Icon';

import styles from './QuickFilterItem.module.css';

interface ILocalProps {
  data: IQuickFilter;
  onSelect: (data: IQuickFilter) => void;
}

export default class QuickFilterItem extends React.Component<ILocalProps> {
  public render() {
    return (
      <div
        className={styles.root}
        onClick={this.props.onSelect.bind(null, this.props.data)}
      >
        <div className={styles.filter_icon}>
          <Icon type="filter" />
        </div>
        <div
          className={styles.prop_text}
          data-test={`quick-filter-item-${this.props.data.propertyName}`}
        >
          {this.props.data.caption || this.props.data.propertyName}
        </div>
      </div>
    );
  }

  @bind
  private onClick() {
    if (this.props.onSelect) {
      this.props.onSelect(this.props.data);
    }
  }
}
