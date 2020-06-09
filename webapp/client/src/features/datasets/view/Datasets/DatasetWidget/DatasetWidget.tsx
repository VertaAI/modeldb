import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';
import { connect } from 'react-redux';
import { Link } from 'react-router-dom';

import DatasetEntityDescriptionManager from 'features/descriptionManager/view/DatasetEntityDescriptionManager/DatasetEntityDescriptionManager';
import DatasetEntityTagsManager from 'features/tagsManager/view/DatasetEntityTagsManager/DatasetEntityTagsManager';
import WithCurrentUserActionsAccesses from 'core/shared/view/domain/WithCurrentUserActionsAccesses/WithCurrentUserActionsAccesses';
import Avatar from 'core/shared/view/elements/Avatar/Avatar';
import Draggable from 'core/shared/view/elements/Draggable/Draggable';
import { Dataset, DatasetType } from 'core/shared/models/Dataset';
import routes from 'routes';

import DatasetBulkDeletion from './DatasetBulkDeletion/DatasetBulkDeletion';
import styles from './DatasetWidget.module.css';
import { unknownUser } from 'core/shared/models/User';

interface ILocalProps {
  dataset: Dataset;
}

type AllProps = ILocalProps;

class DatasetWidget extends React.PureComponent<AllProps> {
  public render() {
    const { dataset } = this.props;

    return (
      <WithCurrentUserActionsAccesses
        entityType="dataset"
        entityId={dataset.id}
        actions={['delete']}
      >
        {({ actionsAccesses }) => (
          <DatasetBulkDeletion
            id={dataset.id}
            isEnabled={actionsAccesses.delete}
          >
            {togglerForDeletion => (
              <div className={cn(styles.root)} data-test="dataset">
                <Link
                  className={styles.dataset_link}
                  to={routes.datasetSummary.getRedirectPathWithCurrentWorkspace(
                    {
                      datasetId: dataset.id,
                    }
                  )}
                >
                  <div className={styles.content}>
                    <div className={styles.title_block}>
                      <div className={styles.title} data-test="dataset-name">
                        {dataset.name}
                      </div>
                      <div className={styles.description}>
                        <span onClick={this.preventRedirect}>
                          <DatasetEntityDescriptionManager
                            datasetId={dataset.id}
                            entityId={dataset.id}
                            entityType="dataset"
                            description={dataset.description}
                          />
                        </span>
                      </div>
                    </div>
                    <div className={styles.tags_block}>
                      <DatasetEntityTagsManager
                        id={dataset.id}
                        datasetId={dataset.id}
                        entityType="dataset"
                        tags={dataset.tags}
                        isDraggableTags={true}
                        onClick={this.onTagsManagerClick}
                      />
                    </div>
                    <div className={styles.owner_block}>
                      <div className={styles.owner_username}>
                        <div>{unknownUser.username}</div>
                        <div className={styles.owner_status}>Owner</div>
                      </div>
                      <Avatar
                        username={unknownUser.username}
                        sizeInPx={36}
                        picture={unknownUser.picture}
                      />
                    </div>
                    <div className={styles.type}>
                      Type:{' '}
                      {(() => {
                        const valueType: Record<DatasetType, string> = {
                          path: 'Path',
                          query: 'Query',
                          raw: 'Raw',
                        };
                        return valueType[dataset.type];
                      })()}
                    </div>
                    <div className={styles.dates_block}>
                      <div className={styles.created_date}>
                        Created: {dataset.dateCreated.toLocaleDateString()}
                      </div>
                      <div>
                        Updated: {dataset.dateUpdated.toLocaleDateString()}
                      </div>
                    </div>
                    <div className={styles.actions}>
                      {togglerForDeletion && (
                        <div
                          className={cn(styles.action, {
                            [styles.action_delete]: true,
                          })}
                          onClick={this.preventRedirect}
                        >
                          {togglerForDeletion}
                        </div>
                      )}
                    </div>
                  </div>
                </Link>
              </div>
            )}
          </DatasetBulkDeletion>
        )}
      </WithCurrentUserActionsAccesses>
    );
  }

  @bind
  private onTagsManagerClick(e: React.MouseEvent, byEmptiness: boolean) {
    !byEmptiness ? this.preventRedirect(e) : undefined;
  }

  @bind
  private preventRedirect(e: React.MouseEvent) {
    e.preventDefault();
  }
}

export default connect()(DatasetWidget);
