import React from 'react';

const LastModified = ({
  lastModifiedAtSource,
}: {
  lastModifiedAtSource: Date;
}) => {
  const formatedDate = `${lastModifiedAtSource.toLocaleDateString()} ${lastModifiedAtSource.toLocaleTimeString()}`;
  return <span title={formatedDate}>{formatedDate}</span>;
};

export default LastModified;
