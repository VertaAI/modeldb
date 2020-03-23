import React from 'react';
import ReactAvatar from 'react-avatar';

import styles from './Avatar.module.css';

interface ILocalProps {
  sizeInPx: number;
  username: string;
  picture: string | undefined;
}

const Avatar: React.FC<ILocalProps> = ({ username, picture, sizeInPx }) => {
  return (
    <ReactAvatar
      name={username}
      round={true}
      size={`${sizeInPx}`}
      textSizeRatio={sizeInPx / 16}
      className={styles.root}
      src={picture || ''}
    />
  );
};

export default React.memo(Avatar);
