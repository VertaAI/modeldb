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
    return (
      <div className={styles.root}>
        <GeneralInfo {...this.props.generalInfo} />
        <DetailsInfo {...this.props.detailsInfo} />
      </div>
    );
  }
}

export default SummaryInfo;
