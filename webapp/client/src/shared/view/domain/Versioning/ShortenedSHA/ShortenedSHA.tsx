import * as React from 'react';

import { SHA } from 'shared/models/Versioning/RepositoryData';

const ShortenedSHA = ({
  sha,
  additionalClassName,
}: {
  sha: SHA;
  additionalClassName?: string;
}) => {
  return (
    <span title={sha} className={additionalClassName}>
      {sha.slice(0, 7)}
    </span>
  );
};

export const shortenSHA = (sha: SHA) => sha.slice(0, 7);

export default ShortenedSHA;
