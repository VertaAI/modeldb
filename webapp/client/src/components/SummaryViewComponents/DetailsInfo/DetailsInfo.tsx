import * as React from 'react';

import SummaryCard from '../shared/SummaryCard/SummaryCard';
import styles from './DetailsInfo.module.css';

interface ILocalProps {
  dateCreated: Date;
  dateUpdated: Date;
  tagsManagerElement: React.ReactElement;
  additionalBlock?: {
    label: string;
    valueElement: React.ReactElement;
  };
}

class DetailsInfo extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      dateCreated,
      dateUpdated,
      additionalBlock,
      tagsManagerElement,
    } = this.props;

    return (
      <SummaryCard title="Details">
        {tagsManagerElement}

        <div className={styles.block}>
          <div className={styles.item}>
            <label className={styles.item_label}>Created</label>
            <div className={styles.item_value}>
              {dateCreated.toLocaleDateString()}
            </div>
          </div>
          <div className={styles.item}>
            <label className={styles.item_label}>Updated</label>
            <div className={styles.item_value}>
              {dateUpdated.toLocaleDateString()}
            </div>
          </div>
        </div>

        {additionalBlock && (
          <div className={styles.additional_block}>
            <div className={styles.item}>
              <label className={styles.item_label}>
                {additionalBlock.label}
              </label>
              <div className={styles.item_value}>
                {additionalBlock.valueElement}
              </div>
            </div>
          </div>
        )}
      </SummaryCard>
    );
  }
}

export type IDetailsInfoLocalProps = ILocalProps;
export default DetailsInfo;
