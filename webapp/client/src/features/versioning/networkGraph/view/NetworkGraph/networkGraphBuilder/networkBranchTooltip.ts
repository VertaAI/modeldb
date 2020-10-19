import * as PIXI from 'pixi.js';

import { INetworkCoordinate } from 'shared/models/Versioning/NetworkGraph';
import { Branch } from 'shared/models/Versioning/RepositoryData';

export const getBranchTooltipGraphics = ({
  branch,
  pointPosition,
}: {
  branch: Branch;
  pointPosition: INetworkCoordinate;
}) => {
  const position = {
    x: pointPosition.x - 10,
    y: pointPosition.y + 15,
  };

  const tooltip = new PIXI.Graphics();

  tooltip
    .beginFill(0x000000, 0.7)
    .drawRoundedRect(position.x, position.y, 20, branch.length * 5.4 + 36, 5)
    .endFill();

  const text = new PIXI.Text(branch, {
    writtingMode: 'vertical-rl',
    fontSize: '12px',
    fill: '#fff',
  });

  text.position = new PIXI.Point(position.x + 18, position.y + 10);
  text.rotation = (Math.PI / 180) * 90;

  tooltip.addChild(text);

  const triange = new PIXI.Graphics();
  triange
    .beginFill(0x000000, 0.7)
    .drawPolygon([
      new PIXI.Point(position.x + 5, position.y),
      new PIXI.Point(position.x + 15, position.y),
      new PIXI.Point(position.x + 10, position.y - 10),
    ])
    .endFill();

  tooltip.addChild(triange);

  return tooltip;
};
