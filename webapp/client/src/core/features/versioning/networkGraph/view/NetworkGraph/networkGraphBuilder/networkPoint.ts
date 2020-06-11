import * as PIXI from 'pixi.js';

import {
  INetworkPoint,
  getNetworkColor,
  networkGraphSettings,
  CursorType,
} from 'core/shared/models/Versioning/NetworkGraph';

import { getBranchTooltipGraphics } from './networkBranchTooltip';
import { getCommitTooltipGraphics } from './networkPointTooltip';

export const getPointGraphics = ({
  point,
  parent,
  changeCursor,
  onPointClick,
}: {
  point: INetworkPoint;
  changeCursor: (type: CursorType) => void;
  parent: any;
  onPointClick: (point: INetworkPoint) => void;
}): PIXI.Graphics => {
  const circle = new PIXI.Graphics();
  const color = getNetworkColor(point.color);

  const commitTooltip = getCommitTooltipGraphics({
    commitSha: point.commitSha,
    message: point.message,
    pointPosition: point.coordinate,
    authorPicture: point.authorPicture,
  });

  circle.sortableChildren = true;

  circle
    .beginFill(color)
    .drawCircle(
      point.coordinate.x,
      point.coordinate.y,
      networkGraphSettings.pointRadius
    )
    .endFill();

  if (point.isLastCommitInBranch) {
    const tooltip = getBranchTooltipGraphics({
      branch: point.branch,
      pointPosition: point.coordinate,
    });
    circle.addChild(tooltip);
  }

  circle.interactive = true;
  circle.hitArea = new PIXI.Circle(
    point.coordinate.x,
    point.coordinate.y,
    networkGraphSettings.bigPointRadius
  );

  circle.addListener('mouseover', e => {
    circle
      .beginFill(color)
      .drawCircle(
        point.coordinate.x,
        point.coordinate.y,
        networkGraphSettings.bigPointRadius
      )
      .endFill();
    parent.addChild(commitTooltip);
    changeCursor('pointer');
  });

  circle.addListener('mouseout', e => {
    circle
      .clear()
      .beginFill(color)
      .drawCircle(
        point.coordinate.x,
        point.coordinate.y,
        networkGraphSettings.pointRadius
      )
      .endFill();
    parent.removeChild(commitTooltip);
    changeCursor('all-scroll');
  });

  circle.addListener('click', e => onPointClick(point));

  return circle;
};
