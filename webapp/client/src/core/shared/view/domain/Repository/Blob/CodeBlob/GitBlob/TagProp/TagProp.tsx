import * as React from 'react';

import { IGitCodeBlob } from 'core/shared/models/Repository/Blob/CodeBlob';
import * as Github from 'core/shared/utils/github/github';
import matchBy from 'core/shared/utils/matchBy';
import ExternalLink from 'core/shared/view/elements/ExternalLink/ExternalLink';

interface ILocalProps {
  tag: string;
  remoteRepoUrl: IGitCodeBlob['data']['remoteRepoUrl'];
  rootStyles?: React.CSSProperties;
}

const TagProp = ({ remoteRepoUrl, tag, rootStyles }: ILocalProps) => {
  return matchBy(remoteRepoUrl, 'type')({
    github: ({ value }) => (
      <ExternalLink
        url={Github.makeTagUrl(value, tag)}
        text={tag}
        rootStyle={rootStyles}
      />
    ),
    unknown: ({ value }) => <span style={rootStyles}>{value}</span>,
  });
};

export default TagProp;
