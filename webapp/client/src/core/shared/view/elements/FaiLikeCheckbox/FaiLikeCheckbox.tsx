import { bind } from 'decko';
import * as React from 'react';

import FaiWithLabel from '../FaiWithLabel/FaiWithLabel';
import { IconType } from '../Icon/Icon';

interface ILocalProps {
  value: boolean;
  iconType: IconType;
  labelWhenUnchecked: string;
  labelWhenChecked: string;
  isDisabled?: boolean;
  dataTest?: string;
  onChange(value: boolean): void;
  onHover?(): void;
  onUnhover?(): void;
}

class FaiLikeCheckbox extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      value,
      iconType,
      isDisabled,
      labelWhenUnchecked,
      labelWhenChecked,
      dataTest,
      onHover,
      onUnhover,
    } = this.props;
    return (
      <FaiWithLabel
        iconType={iconType}
        label={value ? labelWhenChecked : labelWhenUnchecked}
        theme={value ? 'green' : 'blue'}
        isDisabled={isDisabled}
        dataTest={dataTest}
        onClick={this.onChange}
        onHover={onHover}
        onUnhover={onUnhover}
      />
    );
  }

  @bind
  private onChange() {
    this.props.onChange(!this.props.value);
  }
}

export type IFaiLikeCheckboxLocalProps = ILocalProps;
export default FaiLikeCheckbox;
