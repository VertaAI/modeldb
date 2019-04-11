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
            return (
              <Link
                key={i}
                className={cn(styles.model_link, styles.artifact_item)}
                to={routes.modelRecord.getRedirectPath({
                  projectId,
                  modelRecordId: id,
                })}
              >
                <div
                  className={styles.artifact_wrapper}
                  title="view ModelRecord"
                >
                  <div className={styles.notif}>
                    <Icon
                      className={styles.notif_icon}
                      type={artifact.type === 'IMAGE' ? 'image' : 'codepen'}
                    />
                  </div>
                  <div className={styles.type}>
                    {artifact.type} &nbsp; &nbsp; &nbsp; ->{' '}
                  </div>
                  <div className={styles.key}>{artifact.key}</div>
                  <div className={styles.path}>{artifact.path}</div>
                </div>
              </Link>
            );
          })}
      </div>
    );
  }
}

export default ArtifactsColDef;
