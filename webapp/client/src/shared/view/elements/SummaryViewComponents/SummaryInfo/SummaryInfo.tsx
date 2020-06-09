import * as React from 'react';

import DetailsInfo, {
  IDetailsInfoLocalProps,
} from '../DetailsInfo/DetailsInfo';
import GeneralInfo, {
  IGeneralInfoLocalProps,
} from '../GeneralInfo/GeneralInfo';
import styles from './SummaryInfo.module.css';

interface ILocalProps {
  detailsInfo: IDetailsInfoLocalProps;
  generalInfo: IGeneralInfoLocalProps;
}

class SummaryInfo extends React.PureComponent<ILocalProps> {
  public render() {
    const { generalInfo, detailsInfo } = this.props;
    return (
      <div className={styles.root} data-test="summary">
        <GeneralInfo {...generalInfo} />
        <DetailsInfo {...detailsInfo} />
      </div>
    );
  }
}

export default SummaryInfo;
