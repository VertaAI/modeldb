import * as React from 'react';
import styles from './FilterItem.module.css';
import { IFilterData } from './FilterSelect';

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
        <div className={styles.prop_text}>{`${this.props.data.propertyName}${
          this.props.data.propertyValue ? `: ${this.props.data.propertyValue}` : ''
        }`}</div>
        <div className={styles.add_button} onClick={this.onClick}>
          +
        </div>
      </div>
    );
  }
}
