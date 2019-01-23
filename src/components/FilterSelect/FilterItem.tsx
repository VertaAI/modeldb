import * as React from 'react';
import styles from './FilterItem.module.css';
import { IFilterData } from './FilterSelect';

interface ILocalProps {
  PropertyName: string;
  PropertyValue: string;
  onCreateFilter: (data: IFilterData) => void;
}

export default class FilterItem extends React.Component<ILocalProps> {
  public constructor(props: ILocalProps) {
    super(props);
    this.onClick = this.onClick.bind(this);
  }

  public onClick() {
    console.log('sdsdsdsd');
    if (this.props.onCreateFilter) {
      console.log('sss');
      this.props.onCreateFilter({
        propertyName: this.props.PropertyName,
        propertyValue: this.props.PropertyValue
      });
    }
  }

  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.filter_icon}>
          <i className="fa fa-filter" aria-hidden="true" />
        </div>
        <div className={styles.prop_text}>{`${this.props.PropertyName}${
          this.props.PropertyValue ? `: ${this.props.PropertyValue}` : ''
        }`}</div>
        <div className={styles.add_button} onClick={this.onClick}>
          +
        </div>
      </div>
    );
  }
}
