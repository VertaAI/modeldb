import * as PIXI from 'pixi.js';

import {
  INetworkConnection,
  getNetworkColor,
  INetworkBranchConnection,
  INetworkDefaultConnection,
  INetworkMergeConnection,
  INetworkCoordinate,
} from 'shared/models/Versioning/NetworkGraph';
import { exhaustiveCheck } from 'shared/utils/exhaustiveCheck';

type Direction = 'right-top' | 'right-bottom' | 'right';

const getDirection = (
  fromCoordinate: INetworkCoordinate,
  toCoordinate: INetworkCoordinate
): Direction => {
  if (fromCoordinate.y > toCoordinate.y) {
    return 'right-top';
  }

  if (fromCoordinate.y < toCoordinate.y) {
    return 'right-bottom';
  }

  return 'right';
};

const getConnectionArrowCorner = ({
  toCoordinate,
  direction,
}: {
  toCoordinate: INetworkCoordinate;
  direction: Direction;
}): PIXI.Polygon => {
  switch (direction) {
    case 'right':
      return new PIXI.Polygon([
        new PIXI.Point(toCoordinate.x - 11, toCoordinate.y - 2),
        new PIXI.Point(toCoordinate.x - 11, toCoordinate.y + 2),
        new PIXI.Point(toCoordinate.x - 6, toCoordinate.y),
      ]);

    case 'right-top':
      return new PIXI.Polygon([
        new PIXI.Point(toCoordinate.x - 8, toCoordinate.y + 6),
        new PIXI.Point(toCoordinate.x - 6, toCoordinate.y + 8),
        new PIXI.Point(toCoordinate.x - 5, toCoordinate.y + 5),
      ]);

    case 'right-bottom':
      return new PIXI.Polygon([
        new PIXI.Point(toCoordinate.x - 8, toCoordinate.y - 6),
        new PIXI.Point(toCoordinate.x - 6, toCoordinate.y - 8),
        new PIXI.Point(toCoordinate.x - 5, toCoordinate.y - 5),
      ]);

    default:
      exhaustiveCheck(direction, '');
  }
};

const getBranchConnectionGraphics = (connection: INetworkBranchConnection) => {
  const line = new PIXI.Graphics();
  const color = getNetworkColor(connection.color);

  line
    .lineTo(connection.fromCoordinate.x, connection.fromCoordinate.y)
    .lineStyle(2, color)
    .lineTo(connection.fromCoordinate.x, connection.toCoordinate.y)
    .lineTo(connection.toCoordinate.x - 10, connection.toCoordinate.y)
    .beginFill(getNetworkColor(connection.color))
    .drawPolygon(
      getConnectionArrowCorner({
        toCoordinate: connection.toCoordinate,
        direction: 'right',
      })
    )
    .endFill();

  return line;
};

const getDefaultConnectionGraphics = (
  connection: INetworkDefaultConnection
) => {
  const line = new PIXI.Graphics();
  const color = getNetworkColor(connection.color);

  line
    .lineTo(connection.fromCoordinate.x, connection.fromCoordinate.y)
    .lineStyle(2, color)
    .lineTo(connection.fromCoordinate.x, connection.toCoordinate.y)
    .lineTo(connection.toCoordinate.x, connection.toCoordinate.y);

  return line;
};

const getMergeConnectionGraphics = (connection: INetworkMergeConnection) => {
  const line = new PIXI.Graphics();
  const color = getNetworkColor(connection.color);

  const direction = getDirection(
    connection.fromCoordinate,
    connection.toCoordinate
  );

  line
    .lineTo(connection.fromCoordinate.x, connection.fromCoordinate.y)
    .lineStyle(2, color)
    .beginFill(getNetworkColor(connection.color))
    .drawPolygon(
      getConnectionArrowCorner({
        toCoordinate: connection.toCoordinate,
        direction,
      })
    )
    .endFill();

  switch (direction) {
    case 'right':
      throw new Error('Merge connection could not have `right` direction');

    case 'right-top':
      line
        .lineTo(connection.fromCoordinate.x, connection.fromCoordinate.y)
        .lineTo(connection.fromCoordinate.x, connection.toCoordinate.y + 10)
        .lineTo(connection.toCoordinate.x - 10, connection.toCoordinate.y + 10)
        .lineTo(connection.toCoordinate.x - 5, connection.toCoordinate.y + 5);
      break;

    case 'right-bottom':
      line
        .lineTo(connection.fromCoordinate.x, connection.fromCoordinate.y)
        .lineTo(connection.fromCoordinate.x, connection.toCoordinate.y - 10)
        .lineTo(connection.toCoordinate.x - 10, connection.toCoordinate.y - 10)
        .lineTo(connection.toCoordinate.x - 5, connection.toCoordinate.y - 5);
      break;

    default:
      exhaustiveCheck(direction, '');
  }

  return line;
};

export const getConnectionGraphics = (
  connection: INetworkConnection
): PIXI.Graphics => {
  switch (connection.type) {
    case 'DEFAULT': {
      return getDefaultConnectionGraphics(connection);
    }

    case 'BRANCH': {
      return getBranchConnectionGraphics(connection);
    }

    case 'MERGE': {
      return getMergeConnectionGraphics(connection);
    }

    default: {
      exhaustiveCheck(connection, '');
    }
  }
};
