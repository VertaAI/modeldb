import * as React from 'react';
import FilterItem from './FilterItem';
import styles from './FilterSelect.module.css';

interface ILocalProps {
  // onChangeFilter?: React.EventHandler<Map<string, string>>;
  placeHolderText?: string;
  filterProps?: string[];
}

interface ILocalState {
  txt: string;
  filteredProps?: string[];
  isFocused: boolean;
}

export interface IFilterData {
  propertyName: string;
  propertyValue: string;
}

export class FilterSelect extends React.Component<ILocalProps, ILocalState> {
  private static checkProp(txt: string, propName: string) {
    const txtParts = txt
      .trim()
      .toUpperCase()
      .split(' ');
    if (txtParts.length > 1) {
      return (
        propName
          .trim()
          .toUpperCase()
          .search(txtParts[0]) > -1
      );
    }
    return false;
  }

  private static getValue(txt: string): string {
    const txtParts = txt.split(' ');
    if (txtParts.length > 1) {
      txtParts.shift();
      return txtParts.join(' ');
    }
    return '';
  }
  public state: ILocalState = {
    filteredProps: [],
    isFocused: false,
    txt: ''
  };
  public constructor(props: ILocalProps) {
    super(props);
    this.onChange = this.onChange.bind(this);
    this.onFocus = this.onFocus.bind(this);
    this.onBlur = this.onBlur.bind(this);
  }

  public componentDidMount() {
    this.setState({ txt: '', filteredProps: this.props.filterProps });
  }

  public onChange(event: React.ChangeEvent<HTMLInputElement>) {
    let foundProps: string[] | undefined = this.state.filteredProps;
    if (this.props.filterProps) {
      const txt: string = event.target.value;
      foundProps = this.props.filterProps.filter((value, index, array) => FilterSelect.checkProp(txt, value));
    }
    this.setState({ ...this.state, txt: event.target.value, filteredProps: foundProps });
  }

  public onFocus() {
    this.setState({ ...this.state, isFocused: true });
  }

  public onBlur() {
    this.setState({ ...this.state, isFocused: false });
  }

  public render() {
    return (
      <div className={styles.root}>
        <input
          className={styles.input}
          placeholder={this.props.placeHolderText}
          onChange={this.onChange}
          onFocus={this.onFocus}
          onBlur={this.onBlur}
        />
        <label className="fa fa-search" aria-hidden={true} />
        {this.state.filteredProps && this.state.isFocused ? (
          <div className={styles.found_filters_popup}>
            {this.state.filteredProps.map((prop, index) => (
              <FilterItem
                key={index}
                PropertyName={prop}
                PropertyValue={FilterSelect.getValue(this.state.txt)}
                onCreateFilter={this.onCreateFilter}
              />
            ))}
          </div>
        ) : (
          ''
        )}
        <div className={styles.applied_filters} />
        <div className={styles.apply_filters_button}>
          <button>Filter</button>
        </div>
      </div>
    );
  }

  private onCreateFilter(data: IFilterData) {
    console.log(data);
  }
}
