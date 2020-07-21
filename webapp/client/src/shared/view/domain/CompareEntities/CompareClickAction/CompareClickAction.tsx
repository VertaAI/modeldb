import * as React from 'react';

import FaiLikeCheckbox from 'shared/view/elements/FaiLikeCheckbox/FaiLikeCheckbox';

interface ILocalProps {
    isSelected: boolean;
    isDisabled: boolean;
    onHover?(): void;
    onUnhover?(): void;
    onChange: (value: boolean) => void;
}

type AllProps = ILocalProps;

class CompareClickAction extends React.PureComponent<AllProps> {
  public render() {
    const {
      isSelected,
      isDisabled,
      onHover,
      onUnhover,
      onChange
    } = this.props;
    return (
      <FaiLikeCheckbox
        value={isSelected}
        labelWhenChecked={'remove'}
        labelWhenUnchecked={'compare'}
        iconType={'compare-arrows'}
        isDisabled={isDisabled}
        dataTest="comparing-toggler"
        onHover={onHover}
        onUnhover={onUnhover}
        onChange={onChange}
      />
    );
  }
}

export default CompareClickAction;
