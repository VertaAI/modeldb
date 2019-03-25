import * as React from 'react';

import Popup from 'components/shared/Popup/Popup';
import Form from 'components/shared/Form/Form';
import Button from 'components/shared/Button/Button';
import CopyToClipboard from 'components/shared/CopyToClipboard/CopyToClipboard';

import ModelInput from './ModelInput/ModelInput';
import styles from './DeployResult.module.css';

const url = 'https://verta.io/234wfogsfas/fsfbgs';

interface IProps {
  onClose(): void;
}

class DeployResult extends React.PureComponent<IProps> {
  public render() {
    const { onClose } = this.props;
    return (
      <Popup title="Deployed" isOpen={true} onRequestClose={onClose}>
        <div className={styles.deploy_result}>
          <div className={styles.commonInfo}>
            <Form>
              <Form.Item label="Model ID">22</Form.Item>
              <Form.Item label="Type">REST</Form.Item>
              <Form.Item
                label="URL"
                additionalContent={
                  <Button variant="like-link" fullWidth to={url}>
                    <span className={styles.url}>{url}</span>
                  </Button>
                }
              >
                <CopyToClipboard text={url}>
                  {onCopy => (
                    <Button variant="like-link" onClick={onCopy}>
                      Copy
                    </Button>
                  )}
                </CopyToClipboard>
              </Form.Item>
            </Form>
          </div>
          <div className={styles.model_input}>
            <ModelInput />
          </div>
        </div>
      </Popup>
    );
  }
}

export default DeployResult;
