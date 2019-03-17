import * as React from 'react';
import { IFilterData } from '../../../models/Filters';
import styles from './FilterItem.module.css';

interface ILocalProps {
  data: IFilterData;
  onCreateFilter: (data: IFilterData) => void;
}

export default class FilterItem extends React.Component<ILocalProps> {
  public constructor(props: ILocalProps) {
    super(props);
    this.onClick = this.onClick.bind(this);
  }

  public onClick() {
    if (this.props.onCreateFilter) {
      this.props.onCreateFilter(this.props.data);
    }
  }

  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.filter_icon}>
          <i className="fa fa-filter" aria-hidden="true" />
        </div>
        <div className={styles.prop_text}>{`${this.props.data.name}${this.props.data.value ? `: ${this.props.data.value}` : ''}`}</div>
        <div className={styles.add_button} onClick={this.onClick}>
          +
        </div>
      </div>
    );
  }
}
