import React from 'react';

import Tooltip from 'components/shared/Tooltip/Tooltip';

import styles from './ButtonTooltip.module.css';

interface ILocalProps {
  additionalClassName?: string;
  icon: React.ReactNode;
  toolTipContent: string;
  width: number;
  onButtonClick?(): void;
}

export class ButtonTooltip extends React.Component<ILocalProps> {
  public render() {
    const { icon } = this.props;
    return (
      <Tooltip content={this.props.toolTipContent} width={this.props.width}>
        <button
          className={`${styles.tooltip_button} ${
            this.props.additionalClassName ? this.props.additionalClassName : ''
          }`}
          onClick={this.props.onButtonClick}
        >
          <div className={styles.tooltip_button_icon}>{icon}</div>
        </button>
      </Tooltip>
    );
  }
}
