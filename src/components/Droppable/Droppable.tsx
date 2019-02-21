import React from 'react';
import { ConnectDropTarget, DropTarget, DropTargetCollector, DropTargetConnector, DropTargetMonitor, DropTargetSpec } from 'react-dnd';

interface ILocalProps {
  type: string;
  onDrop: (data: any) => void;
}

interface IDropProps {
  isOver: boolean;
  canDrop: boolean;
  connectDropTarget: ConnectDropTarget;
}

const target: DropTargetSpec<ILocalProps> = {
  canDrop(props: ILocalProps) {
    return true;
  },

  drop(props: ILocalProps, monitor: DropTargetMonitor) {
    props.onDrop(monitor.getItem());
  }
};

const collect: DropTargetCollector<IDropProps> = (connect: DropTargetConnector, monitor: DropTargetMonitor) => {
  return {
    canDrop: true,
    connectDropTarget: connect.dropTarget(),
    isOver: !!monitor.isOver()
  };
};

// @DropTarget(props => props.type, target, collect)
class Droppable extends React.Component<ILocalProps & IDropProps> {
  public render() {
    const { connectDropTarget, isOver, canDrop, children } = this.props;
    return connectDropTarget(<div>{children}</div>);
  }
}

export default DropTarget<ILocalProps, IDropProps>(props => props.type, target, collect)(Droppable);
