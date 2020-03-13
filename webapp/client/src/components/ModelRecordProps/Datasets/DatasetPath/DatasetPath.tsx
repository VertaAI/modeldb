import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { formatBytes } from 'core/shared/utils/mapperConverters/DataSizeConverted';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';
import { IDatasetPathPartInfo } from 'models/DatasetVersion';

import styles from './DatasetPath.module.css';

interface ILocalProps {
  datasetUrl: IDatasetPathPartInfo;
  additionalClassname?: string;
}

interface ILocalState {
  isModalOpen: boolean;
}

class DatasetPath extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
  };
  public render() {
    const { datasetUrl, additionalClassname } = this.props;
    const iconType = 'database';
    return (
      <div>
        <Popup
          title="DataSet"
          titleIcon={iconType}
          contentLabel="dataset-action"
          isOpen={this.state.isModalOpen}
          onRequestClose={this.handleCloseModal}
        >
          <div className={styles.popupContent}>
            <div>
              <this.RenderField keystr="Path" value={datasetUrl.path} />
              <this.RenderField
                keystr="Size"
                value={formatBytes(datasetUrl.size)}
              />
              <this.RenderField keystr="Checksum" value={datasetUrl.checkSum} />
            </div>
          </div>
        </Popup>

        <div
          className={cn(styles.attribute_link, styles.attribute_item)}
          onClick={this.showModal}
        >
          <div
            className={cn(styles.attribute_wrapper, additionalClassname)}
            title="view dataset"
          >
            <div className={styles.notif}>
              <Icon className={styles.notif_icon} type={iconType} />
            </div>
            <div className={styles.attributeKey}>
              {datasetUrl.path
                .split('/')
                .slice(-1)
                .pop()}
            </div>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private handleCloseModal() {
    this.setState({ isModalOpen: false });
  }

  @bind
  private showModal() {
    this.setState({ isModalOpen: true });
  }

  private RenderField(props: { keystr?: string; value?: string | number }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value && <div className={styles.fieldValue}>{String(value)}</div>}
      </div>
    );
  }

  private RenderFieldList(props: { keystr?: string; value?: any }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        <div className={styles.listBlock}>
          {value &&
            value.map((val: string) => {
              return <div className={styles.listFieldValue}>{String(val)}</div>;
            })}
        </div>
      </div>
    );
  }

  private RenderFieldObject(props: { keystr?: string; value?: Object }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        <div className={styles.valueContainer}>
          {value && (
            <div className={styles.objectFieldValue}>
              <pre>{JSON.stringify(value, null, 2)}</pre>
            </div>
          )}
        </div>
      </div>
    );
  }
}

export default DatasetPath;
