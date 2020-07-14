import * as React from 'react';

import { IGitCodeBlob } from 'shared/models/Versioning/Blob/CodeBlob';
import * as Github from 'shared/utils/github/github';
import matchBy from 'shared/utils/matchBy';
import ExternalLink from 'shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const RepoProp = ({ remoteRepoUrl, rootStyles }: ILocalProps) => {
  return matchBy(remoteRepoUrl, 'type')({
    github: ({ value }) => (
      <ExternalLink
        url={Github.makeRepoUrl(value)}
        text={Github.makeRepoShortName(value)}
        rootStyle={rootStyles}
      />
    ),
    unknown: ({ value }) => <span style={rootStyles}>{value}</span>,
  });
};

export default RepoProp;
