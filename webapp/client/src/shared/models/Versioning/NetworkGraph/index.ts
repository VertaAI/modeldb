import { SHA, Branch, ICommit } from 'shared/models/Versioning/RepositoryData';

export type NetworkColor = number; // 0xffffff

export interface INetworkCoordinate {
  x: number;
  y: number;
}

export interface INetworkData {
  points: INetworkPoint[];
  connections: INetworkConnection[];
}

export interface INetworkPoint {
  coordinate: INetworkCoordinate;
  branch: Branch;
  message: ICommit['message'];
  color: NetworkColor;
  isLastCommitInBranch: boolean;
  commitSha: SHA;
  authorPicture: string | null;
}

export type INetworkConnection =
  | INetworkDefaultConnection
  | INetworkMergeConnection
  | INetworkBranchConnection;

export interface INetworkBranchConnection {
  type: 'BRANCH';
  toCoordinate: INetworkCoordinate;
  fromCoordinate: INetworkCoordinate;
  color: NetworkColor;
}

export interface INetworkMergeConnection {
  type: 'MERGE';
  toCoordinate: INetworkCoordinate;
  fromCoordinate: INetworkCoordinate;
  color: NetworkColor;
}

export interface INetworkDefaultConnection {
  type: 'DEFAULT';
  toCoordinate: INetworkCoordinate;
  fromCoordinate: INetworkCoordinate;
  color: NetworkColor;
}

const colors: NetworkColor[] = [
  0x000000,
  0x8e44ad,
  0xf1c40f,
  0x2980b9,
  0xe67e22,
  0x3498db,
  0x34495e,
  0x0dc400,
  0xff0000,
  0x00ffed,
  0x3f3fff,
  0xa1ff00,
];

export const getNetworkColor = (index: number): NetworkColor =>
  colors[index % colors.length];

export const networkGraphSettings = {
  startPaddingX: 20,
  startPaddingY: 20,
  xPointGap: 20,
  yPointGap: 20,
  pointRadius: 3,
  bigPointRadius: 6,
};

export type CursorType = 'all-scroll' | 'pointer';
