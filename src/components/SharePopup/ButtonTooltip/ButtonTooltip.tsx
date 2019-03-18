import Tooltip from 'rc-tooltip';
import React from 'react';
import styles from './ButtonTooltip.module.css';
import './tooltip.css';

interface ILocalProps {
  additionalClassName?: string;
  imgSrc: string;
  toolTipContent: string;
  width: number;
  onButtonClick?(): void;
}

export class ButtonTooltip extends React.Component<ILocalProps> {
  public render() {
    return (
      <Tooltip
        overlay={this.props.toolTipContent}
        placement={'top'}
        arrowContent={<div className="rc-tooltip-arrow-inner" />}
        align={{
          offset: [0, -9]
        }}
        overlayStyle={{ width: this.props.width }}
        mouseEnterDelay={0.1}
        destroyTooltipOnHide={true}
      >
        <button
          className={`${styles.tooltip_button} ${this.props.additionalClassName ? this.props.additionalClassName : ''}`}
          onClick={this.props.onButtonClick}
        >
          <img className={styles.tooltip_button_icon} src={this.props.imgSrc} />
        </button>
      </Tooltip>
    );
  }
}
