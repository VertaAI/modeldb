import * as PIXI from 'pixi.js';

import {
  INetworkPoint,
  INetworkCoordinate,
} from 'shared/models/Versioning/NetworkGraph';
import { SHA } from 'shared/models/Versioning/RepositoryData';

export const getCommitTooltipGraphics = ({
  commitSha,
  message,
  pointPosition,
  authorPicture,
}: {
  commitSha: SHA;
  message: string;
  pointPosition: INetworkCoordinate;
  authorPicture: INetworkPoint['authorPicture'];
}) => {
  const position = {
    x: pointPosition.x + 10,
    y: pointPosition.y + 10,
  };

  const height = Math.ceil(message.length / 19) * 12 + 18;

  const tooltip = new PIXI.Graphics();
  tooltip
    .beginFill(0xffffff)
    .lineStyle(1, 0xcccccc)
    .drawRoundedRect(position.x, position.y, 200, height, 5)
    .endFill();
  tooltip.zIndex = 10;

  if (authorPicture) {
    const avatar = getAvatarSprite({
      src: authorPicture,
      position: {
        x: position.x + 6,
        y: position.y + 6,
      },
      height: 20,
      width: 20,
    });

    tooltip.addChild(avatar);
  }

  const text = new PIXI.Text(message, {
    fontSize: '12px',
    wordWrap: true,
    wordWrapWidth: 120,
  });
  text.position = new PIXI.Point(position.x + 30, position.y + 8);

  const shaText = new PIXI.Text(commitSha.slice(0, 7), {
    fontSize: '11px',
    fill: 0xbbbbbb,
  });
  shaText.position = new PIXI.Point(position.x + 150, position.y + 9);

  tooltip.addChild(text, shaText);

  return tooltip;
};

const getAvatarSprite = ({
  src,
  width,
  height,
  position,
}: {
  src: string;
  width: number;
  height: number;
  position: INetworkCoordinate;
}) => {
  const texture = PIXI.Texture.from(src);
  const avatar = new PIXI.Sprite(texture);
  avatar.width = width;
  avatar.height = height;
  avatar.x = position.x;
  avatar.y = position.y;

  return avatar;
};
