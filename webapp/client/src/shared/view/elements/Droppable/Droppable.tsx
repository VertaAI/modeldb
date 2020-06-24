import { bind } from 'decko';
import React from 'react';

interface ILocalProps {
  type: string;
  onDrop: (data: any) => void;
}

export default class Droppable extends React.Component<ILocalProps> {
  public render() {
    return (
      <div
        onDrop={this.onDrop}
        onDragEnter={this.onDragEnter}
        onDragOver={this.onDragOver}
      >
        {this.props.children}
      </div>
    );
  }

  @bind
  private onDrop(e: React.DragEvent) {
    this.props.onDrop(JSON.parse(e.dataTransfer.getData(this.props.type)));
    e.stopPropagation();
  }

  @bind
  private onDragEnter(e: React.DragEvent) {
    return true;
  }

  @bind
  private onDragOver(e: React.DragEvent) {
    if (e.dataTransfer.types.includes(this.props.type)) {
      e.preventDefault();
    }
  }
}
