import * as React from 'react';
import Dropdown, { Option } from 'react-dropdown';

import 'react-dropdown/style.css';
import styles from './ChartConfigDropdown.module.css';

interface ILocalProps {
  value: string;
  options: string[] | Set<string>;
  label: string;
  isDeployDropdown?: boolean;
  isDisabled?: boolean;
  onChange(option: IOption): void;
}

export type IOption = Option;

class ChartConfigDropdown extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      isDisabled,
      label,
      value,
      options,
      isDeployDropdown,
      onChange,
    } = this.props;
    return (
      <div
        className={
          isDeployDropdown
            ? styles.dropdown_container_deploy
            : styles.dropdown_container
        }
      >
        <span className={styles.dropdown_label}>{label}</span>
        <Dropdown
          value={value}
          options={[...options]}
          disabled={isDisabled}
          controlClassName={styles.dropdown_control}
          placeholderClassName={styles.dropdown_placeholder}
          menuClassName={styles.dropdown_menu}
          placeholder="Select an option"
          onChange={onChange}
        />
      </div>
    );
  }
}

export default ChartConfigDropdown;
