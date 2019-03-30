import * as React from 'react';

import Button from 'components/shared/Button/Button';
import CopyToClipboard from 'components/shared/CopyToClipboard/CopyToClipboard';
import Form from 'components/shared/Form/Form';
import Popup from 'components/shared/Popup/Popup';
import { IDeployedStatusInfo } from 'models/Deploy';

import styles from './DeployResult.module.css';

interface ILocalProps {
  modelId: string;
  data: IDeployedStatusInfo['data'];
  onClose(): void;
}

type AllProps = ILocalProps;

const mockApi = 'https://verta.io/234wfogsfas/fsfbgs';

class DeployResult extends React.Component<AllProps> {
  public render() {
    const {
      data: { api = mockApi },
      modelId,
      onClose,
    } = this.props;
    return (
      <Popup title="Deployed" isOpen={true} onRequestClose={onClose}>
        <div className={styles.deploy_result}>
          <div className={styles.commonInfo}>
            <Form>
              <Form.Item label="Model ID">
                <div className={styles.model_id}>{modelId}</div>
              </Form.Item>
              <Form.Item label="Type">REST</Form.Item>
              <Form.Item
                label="URL"
                additionalContent={
                  <Button variant="like-link" fullWidth={true} to={api}>
                    <span className={styles.url}>{api}</span>
                  </Button>
                }
              >
                <CopyToClipboard text={api}>
                  {onCopy => (
                    <Button variant="like-link" onClick={onCopy}>
                      Copy
                    </Button>
                  )}
                </CopyToClipboard>
              </Form.Item>
            </Form>
          </div>
          {/* <div className={styles.model_input}>
            <ModelInput input={modelApi.input} />
          </div> */}
        </div>
      </Popup>
    );
  }
}

export default DeployResult;
