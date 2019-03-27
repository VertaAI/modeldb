import { bind } from 'decko';
import React from 'react';

interface ILocalProps {
  type: string;
  data: object;
  additionalClassName?: string | '';
}

export default class Draggable extends React.Component<ILocalProps> {
  public render() {
    return (
      <div
        style={{ cursor: 'move' }}
        draggable={true}
        onDragStart={this.onDragStart}
        className={this.props.additionalClassName}
      >
        {this.props.children}
      </div>
    );
  }

  @bind
  private onDragStart(e: React.DragEvent) {
    e.dataTransfer.effectAllowed = 'move';
    e.dataTransfer.setData(this.props.type, JSON.stringify(this.props.data));
    return true;
  }
}
