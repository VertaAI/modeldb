import * as React from 'react';
import styles from './AppliedFilterItem.module.css';
import { IFilterData } from './FilterSelect';

interface ILocalProps {
  data: IFilterData;
  // onCreateFilter: (data: IFilterData) => void;
}

export default class AppliedFilterItem extends React.Component<ILocalProps> {
  public constructor(props: ILocalProps) {
    super(props);
    // this.onClick = this.onClick.bind(this);
  }

  // public onClick() {
  //   console.log('sdsdsdsd');
  //   if (this.props.onCreateFilter) {
  //     console.log('sss');
  //     this.props.onCreateFilter({
  //       propertyName: this.props.PropertyName,
  //       propertyValue: this.props.PropertyValue
  //     });
  //   }
  // }

  public render() {
    return (
      <div className={styles.root}>
        <div className={styles.remove_button}>{/* <i className="fa fa-filter" aria-hidden="true" /> */}x</div>
        <div className={styles.filter_text}>{`${this.props.data.propertyName}${
          this.props.data.propertyValue ? `: ${this.props.data.propertyValue}` : ''
        }`}</div>
        <div className={styles.edit_button}>
          <i className="fa fa-caret-down" aria-hidden="true" />
        </div>
      </div>
    );
  }
}
