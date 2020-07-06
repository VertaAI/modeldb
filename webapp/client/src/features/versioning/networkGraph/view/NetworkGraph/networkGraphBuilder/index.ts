import { Scrollbox } from 'pixi-scrollbox';
import * as PIXI from 'pixi.js';
import * as R from 'ramda';

import {
  INetworkData,
  INetworkPoint,
  CursorType,
} from 'shared/models/Versioning/NetworkGraph';
import { getConnectionGraphics } from './networkConnection';
import { getPointGraphics } from './networkPoint';

export const initGraph = ({
  data,
  container,
  changeCursor,
  onPointClick,
}: {
  data: INetworkData;
  container: HTMLDivElement;
  changeCursor: (type: CursorType) => void;
  onPointClick: (point: INetworkPoint) => void;
}) => {
  PIXI.utils.skipHello();
  const app = new PIXI.Application({
    backgroundColor: 0xffffff,
    width: 915,
    antialias: true,
  });
  container.appendChild(app.view);

  const { points, connections } = data;

  const scrollbox = new Scrollbox({
    boxWidth: 915,
    boxHeight: 600,
    overflowX:
      points.length && points[points.length - 1].coordinate.x > 700
        ? 'scroll'
        : 'auto',
    overflowY:
      Math.max(...points.map(p => p.coordinate.y)) > 300 ? 'scroll' : 'auto',
  });

  scrollbox.activateFade();
  scrollbox.update();

  const pointsGraphics = points.map(point =>
    getPointGraphics({
      point,
      changeCursor,
      parent: scrollbox.content,
      onPointClick,
    })
  );
  const connectionsGraphics = connections.map(getConnectionGraphics);

  const padding = new PIXI.Graphics();
  if (points.length > 0) {
    padding.lineTo(
      R.last(points)!.coordinate.x + 300,
      Math.max(...points.map(p => p.coordinate.y)) + 300
    );
  }

  scrollbox.content.sortableChildren = true;

  scrollbox.content.addChild(
    ...connectionsGraphics,
    ...pointsGraphics,
    padding
  );

  app.stage.addChild(scrollbox);

  return () => app.destroy();
};
