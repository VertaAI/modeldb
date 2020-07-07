import * as React from 'react';
import cn from 'classnames';

import { IconType, Icon } from '../Icon/Icon';
import styles from './PileWithActions.module.css';
import Preloader from '../Preloader/Preloader';

interface ILocalProps {
  pile: IPileLocalProps;
  isShowPreloader: boolean;
  actions: JSX.Element[];
}

const PileWithActions = (props: ILocalProps) => {
  const { pile, actions, isShowPreloader } = props;
  return (
    <div className={styles.root}>
      <div className={styles.pileContainer}>
        <Pile {...pile} />
      </div>
      <div className={styles.actions}>{actions.map(action => action)}</div>
      {isShowPreloader && (
        <div className={styles.preloader}>
          <Preloader dynamicSize={true} theme="blue" variant="circle" />
        </div>
      )}
    </div>
  );
};

export const Action = ({
  iconType,
  onClick,
}: {
  iconType: IconType;
  onClick: () => void;
}) => {
  return <Icon className={styles.action} type={iconType} onClick={onClick} />;
};

interface IPileLocalProps {
  additionalClassname?: string;
  title: string;
  label: string;
  iconType: IconType;
  dataTest?: string;
  labelDataTest?: string;
}

const Pile = (props: IPileLocalProps) => {
  const {
    additionalClassname,
    label,
    labelDataTest,
    title,
    dataTest,
    iconType,
  } = props;
  return (
    <div
      className={cn(styles.pile, additionalClassname)}
      title={title}
      data-test={dataTest}
    >
      <div className={styles.notif}>
        <Icon className={styles.notif_icon} type={iconType} />
      </div>
      <div className={styles.label} data-test={labelDataTest}>
        {label}
      </div>
    </div>
  );
};

export default PileWithActions;
