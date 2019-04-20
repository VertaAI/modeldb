import cn from 'classnames';
import * as React from 'react';
import { Link } from 'react-router-dom';

import Icon from 'components/shared/Icon/Icon';
import routes from 'routes';

import styles from './ColumnDefs.module.css';

class ArtifactsColDef extends React.Component<any> {
  public render() {
    const { artifacts, id, projectId } = this.props.data;
    return (
      <div>
        {artifacts &&
          artifacts.map((artifact: any, i: number) => {
            const icon = (() => {
              if (artifact.type === 'IMAGE') {
                return 'image';
              }
              if (artifact.type === 'BINARY') {
                return 'cube';
              }
              return 'codepen';
            })();
            return (
              <Link
                key={i}
                className={cn(styles.model_link, styles.artifact_item)}
                to={routes.modelRecord.getRedirectPath({
                  projectId,
                  modelRecordId: id,
                })}
              >
                <div className={styles.artifact_wrapper} title="view Artifacts">
                  <div className={styles.notif}>
                    <Icon className={styles.notif_icon} type={icon} />
                  </div>
                  <div className={styles.artifactKey}>{artifact.key}</div>
                </div>
              </Link>
            );
          })}
      </div>
    );
  }
}

export default ArtifactsColDef;
