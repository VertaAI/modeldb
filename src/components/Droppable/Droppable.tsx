import React from 'react';

interface ILocalProps {
  type: string;
  onDrop: (data: any) => void;
}

export default class Droppable extends React.Component<ILocalProps> {
  constructor(props: ILocalProps) {
    super(props);
    this.onDrop = this.onDrop.bind(this);
    this.onDragEnter = this.onDragEnter.bind(this);
    this.onDragOver = this.onDragOver.bind(this);
  }

  public render() {
    return (
      <div onDrop={this.onDrop} onDragEnter={this.onDragEnter} onDragOver={this.onDragOver}>
        {this.props.children}
      </div>
    );
  }

  private onDrop(e: React.DragEvent) {
    this.props.onDrop(JSON.parse(e.dataTransfer.getData(this.props.type)));
    e.stopPropagation();
  }

  private onDragEnter(e: React.DragEvent) {
    return true;
  }

  private onDragOver(e: React.DragEvent) {
    if (e.dataTransfer.types.includes(this.props.type)) {
      e.preventDefault();
    }
  }
}
