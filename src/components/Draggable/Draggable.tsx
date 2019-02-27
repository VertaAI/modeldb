import React from 'react';
import {
  ConnectDragPreview,
  ConnectDragSource,
  DragSource,
  DragSourceCollector,
  DragSourceConnector,
  DragSourceMonitor,
  DragSourceSpec
} from 'react-dnd';

interface ILocalProps {
  type: string;
  data: object;
  additionalClassName?: string | '';
}

interface IDragProps {
  isDragginig?: boolean;
  connectDragSource: ConnectDragSource;
  connectDragPreview: ConnectDragPreview;
}

const source: DragSourceSpec<ILocalProps, object> = {
  beginDrag(props: ILocalProps) {
    return props.data;
  }
};

const collect: DragSourceCollector<IDragProps> = (connect: DragSourceConnector, monitor: DragSourceMonitor) => {
  return {
    connectDragPreview: connect.dragPreview(),
    connectDragSource: connect.dragSource(),
    isDragginig: monitor.isDragging()
  };
};

class Draggable extends React.Component<ILocalProps & IDragProps> {
  public render() {
    const { connectDragSource, children } = this.props;
    return connectDragSource(<div className={this.props.additionalClassName}>{children}</div>);
  }
}

export default DragSource<ILocalProps, IDragProps>(props => props.type, source, collect)(Draggable);
