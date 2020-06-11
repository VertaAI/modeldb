import { Location } from 'history';
import React, { useEffect } from 'react';
import { useHistory, useLocation } from 'react-router';

interface ILocalProps {
  children: React.ReactNode;
  onReload: () => void;
}

const locationToString = (location: Location) =>
  `${location.pathname}/${location.search}`;

const Reloading: React.FC<ILocalProps> = ({ children, onReload }) => {
  const history = useHistory();
  const location = useLocation();

  useEffect(() => {
    const unlisten = history.listen(newLocation => {
      if (locationToString(location) === locationToString(newLocation)) {
        onReload();
      }
    });

    return () => {
      unlisten();
    };
  }, [onReload]);

  return <>{children}</>;
};

export default Reloading;
