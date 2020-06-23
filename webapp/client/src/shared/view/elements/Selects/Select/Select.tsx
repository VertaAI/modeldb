import React, { useState } from 'react';
import cn from 'classnames';
import ReactSelect, {
  Props as ReactSelectProps,
  components,
  OptionProps,
  OptionTypeBase,
} from 'react-select';
import Creatable from 'react-select/creatable';
import matchBy from 'shared/utils/matchBy';

import styles from './Select.module.css';
import { IconType, Icon } from '../../Icon/Icon';

export interface IOptionType<Value = string> {
  label: string;
  value: Value;
  iconType?: IconType;
  title?: string;
}

interface ICustomOptionProps<T = OptionTypeBase> extends OptionProps<T> {
  data: IOptionType<T>;
}

const Option: React.FC<ICustomOptionProps> = props => {
  const { data, isSelected, children } = props;
  return (
    <components.Option {...props}>
      {isSelected && (
        <Icon type="checkmark" className={styles.optionCheckmark} />
      )}
      <span className={styles.optionLabel} title={data.title}>
        {children}
      </span>
      {data.iconType && (
        <Icon className={styles.optionIcon} type={data.iconType} />
      )}
    </components.Option>
  );
};

type ISelectLocalProps<T> = Omit<
  ReactSelectProps<IOptionType<T>>,
  'value' | 'onChange' | 'isMulti' | 'styles'
> & {
  value: IOptionType<T> | undefined;
  isMenuWithDynamicWidth?: boolean;
  onChange(option: IOptionType<T>): void;
  options: Array<IOptionType<T>>;
  inputText?: string;
  customStyles?: {
    menu: Pick<React.CSSProperties, 'width'> & { position: 'left' | 'right' };
  };
} & (
    | { isCreatable: true; onCreateOption: (value: string) => void }
    | { isCreatable?: false; onCreateOption?: never });

const Select = <T extends any = string>(props: ISelectLocalProps<T>) => {
  /* it need only for test purpose so that we can open select from tests */
  const [isOpen, setState] = useState<boolean | undefined>(undefined);

  const inputId = 'clickableInput';
  const commonProps: React.ComponentProps<typeof ReactSelect> = {
    ...props,
    isSearchable: props.inputText ? false : undefined,
    inputId,
    styles: props.customStyles
      ? {
          menu: base => {
            return {
              ...base,
              width: `${props.customStyles!.menu.width} !important`,
              ...matchBy(props.customStyles!.menu, 'position')<
                React.CSSProperties
              >({
                left: () => ({ left: 0 }),
                right: () => ({ right: 0 }),
              }),
            };
          },
        }
      : undefined,
    components: {
      Option,
      ValueContainer: props.inputText
        ? valueContainerProps => (
            <components.ValueContainer {...valueContainerProps}>
              {props.inputText}
              {React.Children.map(valueContainerProps.children, child => {
                return child && (child as any).props.id === inputId
                  ? child
                  : null;
              })}
            </components.ValueContainer>
          )
        : components.ValueContainer,
    },
    className: cn(styles.root, {
      [styles.menuWithDynamicWidth]: props.isMenuWithDynamicWidth,
    }),
    classNamePrefix: 'react-select',
    menuIsOpen: (isOpen === undefined ? undefined : isOpen) as any,
    value: props.value || ('' as any),
    'data-test': props.name,
    onChange: (value, actionMeta) => {
      if (actionMeta.action === 'select-option' && value) {
        props.onChange!(value as any);
      }
    },
  };

  return (
    <>
      {/* it need only for test purpose so that we can open select from tests */}
      <div
        data-select-name={props.name}
        data-test="react-select-open"
        onClick={() => setState(true)}
      />
      {props.isCreatable ? (
        <Creatable {...commonProps} onCreateOption={props.onCreateOption} />
      ) : (
        <ReactSelect {...commonProps} />
      )}
    </>
  );
};

export default Select;
