import * as React from 'react';

import CopyButton from 'core/shared/view/elements/CopyButton/CopyButton';
import IdView from 'core/shared/view/elements/IdView/IdView';

import SummaryCard from '../shared/SummaryCard/SummaryCard';
import styles from './GeneralInfo.module.css';

interface ILocalProps {
  id: string;
  descriptionManagerElement: React.ReactElement;
}

class GeneralInfo extends React.PureComponent<ILocalProps> {
  public render() {
    const { id, descriptionManagerElement } = this.props;

    return (
      <SummaryCard title="Description">
        {descriptionManagerElement}
        <div className={styles.id_block}>
          <span className={styles.id_label}>ID</span>
          <IdView
            dataTest={'summary-info-id'}
            value={id}
            title={id}
            additionalClassName={styles.id_value}
          />
          <CopyButton value={id} />
        </div>
      </SummaryCard>
    );
  }
}

export type IGeneralInfoLocalProps = ILocalProps;
export default GeneralInfo;
