import * as React from 'react';
import styles from './AppliedFilterItem.module.css';
import { IFilterData } from './FilterSelect';

interface ILocalProps {
  data: IFilterData;
  onRemoveFilter?: (data: IFilterData) => void;
}

export default class AppliedFilterItem extends React.Component<ILocalProps> {
  public constructor(props: ILocalProps) {
    super(props);
    this.onClickRemove = this.onClickRemove.bind(this);
  }

  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.remove_button} onClick={this.onClickRemove}>
          {/* <i className="fa fa-filter" aria-hidden="true" /> */}x
        </div>
        <div className={styles.filter_text}>{`${this.props.data.propertyName}${
          this.props.data.propertyValue ? `: ${this.props.data.propertyValue}` : ''
        }`}</div>
        <div className={styles.edit_button}>
          <i className="fa fa-caret-down" aria-hidden="true" />
        </div>
      </div>
    );
  }

  private onClickRemove() {
    if (this.props.onRemoveFilter) {
      this.props.onRemoveFilter(this.props.data);
    }
  }
}
