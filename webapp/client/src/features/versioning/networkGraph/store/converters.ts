import {
  INetworkPoint,
  networkGraphSettings,
  INetworkConnection,
  INetworkData,
} from 'core/shared/models/Versioning/NetworkGraph';

import { Network_workspace_repository_network } from './graphql-types/Network';

export const convertNetwork = (
  data: Network_workspace_repository_network
): INetworkData => {
  const points: INetworkPoint[] = data.commits.map((commit, commitIndex) => {
    const branchIndex = data.branches.findIndex(b => b.color === commit.color);
    const branch = data.branches[branchIndex];

    return {
      branch: branch ? branch.branch : '',
      message: commit.commit.message || '',
      color: commit.color,
      isLastCommitInBranch: branch && branch.commitIndex === commitIndex,
      coordinate: {
        x:
          networkGraphSettings.startPaddingX +
          commitIndex * networkGraphSettings.xPointGap,
        y:
          networkGraphSettings.startPaddingY +
          commit.color * networkGraphSettings.yPointGap,
      },
      commitSha: commit.commit.sha,
      authorPicture: commit.commit.author.picture,
    };
  });

  const connections: INetworkConnection[] = data.edges.map(edge => {
    const fromPoint = points[edge.fromCommitIndex];
    const toPoint = points[edge.toCommitIndex];

    return {
      type: edge.edgeType as INetworkConnection['type'],
      color: edge.color,
      fromCoordinate: fromPoint.coordinate,
      toCoordinate: toPoint.coordinate,
    };
  });

  return {
    points,
    connections,
  };
};
