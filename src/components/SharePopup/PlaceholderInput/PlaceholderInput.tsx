import React from 'react';
import styles from './PlaceholderInput.module.css';

interface ILocalProps {
  additionalClassName?: string;
  additionalControl?: JSX.Element;
  inputValue: string;
  placeholderValue: string;
  onInputChange(event: React.ChangeEvent<HTMLInputElement>): void;
}

export class PlaceholderInput extends React.Component<ILocalProps> {
  private input?: HTMLInputElement;
  constructor(props: ILocalProps) {
    super(props);

    this.focusOnInputLabelClick = this.focusOnInputLabelClick.bind(this);
  }

  public render() {
    return (
      <label
        className={`${styles.form_group} ${styles.content_label} ${this.props.additionalClassName}`}
        onClick={this.focusOnInputLabelClick}
      >
        <input
          type="text"
          placeholder=" "
          className={styles.content_input}
          value={this.props.inputValue}
          onChange={this.props.onInputChange}
          ref={c => (this.input = c!)}
        />
        <label className={`${styles.content_label} ${styles.content_placeholder}`}>{this.props.placeholderValue}</label>
        {this.props.additionalControl}
      </label>
    );
  }

  private focusOnInputLabelClick() {
    this.input!.focus();
  }
}
