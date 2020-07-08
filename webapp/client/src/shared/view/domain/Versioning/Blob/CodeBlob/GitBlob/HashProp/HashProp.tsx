import * as React from 'react';

import { IGitCodeBlob } from 'shared/models/Versioning/Blob/CodeBlob';
import { ICommit } from 'shared/models/Versioning/RepositoryData';
import * as Github from 'shared/utils/github/github';
import matchBy from 'shared/utils/matchBy';
import ExternalLink from 'shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  commitHash: ICommit['sha'];
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const HashProp = ({ remoteRepoUrl, commitHash, rootStyles }: ILocalProps) => {
  return (
    <span data-test="git-hash" data-root-styles={rootStyles}>
      {matchBy(remoteRepoUrl, 'type')({
        github: ({ value }) => (
          <ExternalLink
            url={Github.makeCommitUrl(value, commitHash)}
            text={commitHash}
            rootStyle={rootStyles}
          />
        ),
        unknown: () => <span style={rootStyles}>{commitHash}</span>,
      })}
    </span>
  );
};

export default HashProp;
