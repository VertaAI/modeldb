import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Versioning/Blob/CodeBlob';
import { ICommit } from 'core/shared/models/Versioning/RepositoryData';
import * as Github from 'core/shared/utils/github/github';
import matchBy from 'core/shared/utils/matchBy';
import ExternalLink from 'core/shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  commitHash: ICommit['sha'];
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const HashProp = ({ remoteRepoUrl, commitHash, rootStyles }: ILocalProps) => {
  return matchBy(remoteRepoUrl, 'type')({
    github: ({ value }) => (
      <ExternalLink
        url={Github.makeCommitUrl(value, commitHash)}
        text={commitHash}
        rootStyle={rootStyles}
      />
    ),
    unknown: ({ value }) => <span style={rootStyles}>{value}</span>,
  });
};

export default HashProp;
