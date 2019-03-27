import React from 'react';
import { bind } from 'decko';
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

  @bind
  private focusOnInputLabelClick() {
    this.input!.focus();
  }
}
