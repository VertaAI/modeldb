import * as React from 'react';

import Button from '../Button/Button';
import styles from './Dropdown.module.css';
import ClickOutsideListener from '../ClickOutsideListener/ClickOutsideListener';
import { Icon } from '../Icon/Icon';

interface ILocalProps {
  dataTest: string;
  button: {
    text: string;
    isDisabled?: boolean;
    isLoading?: boolean;
  };
  menuItems: [IMenuItem, ...IMenuItem[]];
}

interface IMenuItem {
  text: string;
  wrapper?: (menuItem: React.ReactNode, key: string) => React.ReactNode;
  onClick?: () => void;
}

const Dropdown = ({ button, menuItems, dataTest }: ILocalProps) => {
  const [isShowMenu, changeIsShowMenu] = React.useState(false);

  return (
    <ClickOutsideListener
      onClickOutside={() => (isShowMenu ? changeIsShowMenu(false) : undefined)}
    >
      <div className={styles.root} data-test={dataTest}>
        <div className={styles.button}>
          <Button
            fullHeight={true}
            fullWidth={true}
            disabled={button.isDisabled}
            isLoading={button.isLoading}
            dataTest="dropdown-button"
            onClick={() => changeIsShowMenu(true)}
          >
            <div className={styles.button__content}>
              {button.text}
              <Icon className={styles.button__icon} type="caret-down" />
            </div>
          </Button>
        </div>
        {isShowMenu && (
          <div className={styles.menu}>
            {menuItems.map(
              (
                {
                  text,
                  onClick,
                  wrapper = (x: React.ReactNode, key: string) => (
                    <div key={key}>{x}</div>
                  ),
                },
                i
              ) =>
                wrapper(
                  <div
                    onClick={() => {
                      changeIsShowMenu(false);
                      if (onClick) {
                        onClick();
                      }
                    }}
                    data-test="dropdown-menu-item"
                    className={styles.menu__item}
                  >
                    {text}
                  </div>,
                  String(i)
                )
            )}
          </div>
        )}
      </div>
    </ClickOutsideListener>
  );
};

export default Dropdown;
