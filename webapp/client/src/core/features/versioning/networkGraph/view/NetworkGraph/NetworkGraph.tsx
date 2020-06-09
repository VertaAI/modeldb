import React, { useRef, useEffect, useState, useCallback } from 'react';
import { useHistory } from 'react-router';

import {
  CursorType,
  INetworkPoint,
  INetworkData,
} from 'core/shared/models/Versioning/NetworkGraph';
import { IRepository } from 'core/shared/models/Versioning/Repository';
import DefaultMatchRemoteData from 'core/shared/view/elements/MatchRemoteDataComponents/DefaultMatchRemoteData';
import { IWorkspace } from 'core/shared/models/Workspace';
import routes from 'routes';

import { useNetworkQuery } from '../../store/query';
import styles from './NetworkGraph.module.css';
import { initGraph } from './networkGraphBuilder';

interface ILocalProps {
  workspaceName: IWorkspace['name'];
  repositoryName: IRepository['name'];
}

const NetworkGraph: React.FC<ILocalProps> = ({
  workspaceName,
  repositoryName,
}) => {
  const networkQuery = useNetworkQuery({ workspaceName, repositoryName });

  return (
    <React.Fragment>
      <DefaultMatchRemoteData
        data={networkQuery.data}
        communication={networkQuery.communication}
      >
        {data => (
          <NetworkGraphView
            networkData={data}
            repositoryName={repositoryName}
            workspaceName={workspaceName}
          />
        )}
      </DefaultMatchRemoteData>
    </React.Fragment>
  );
};

const NetworkGraphView: React.FC<{
  networkData: INetworkData;
  repositoryName: IRepository['name'];
  workspaceName: IWorkspace['name'];
}> = ({ networkData, workspaceName, repositoryName }) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const [cursor, changeCursor] = useState<CursorType>('all-scroll');
  const history = useHistory();

  const onPointClick = useCallback(
    (point: INetworkPoint) => {
      history.push(
        routes.repositoryCommit.getRedirectPath({
          commitSha: point.commitSha,
          repositoryName,
          workspaceName,
        })
      );
    },
    [repositoryName, workspaceName]
  );

  useEffect(() => {
    if (containerRef.current) {
      return initGraph({
        data: networkData,
        container: containerRef.current,
        changeCursor,
        onPointClick,
      });
    }
  }, [networkData, onPointClick, changeCursor]);

  return <div className={styles.root} ref={containerRef} style={{ cursor }} />;
};

export default NetworkGraph;
