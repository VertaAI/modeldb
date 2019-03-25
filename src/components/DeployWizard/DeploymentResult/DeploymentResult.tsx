import * as React from 'react';

import Popup from 'components/shared/Popup/Popup';
import Form from 'components/shared/Form/Form';

import ModelInput from './ModelInput/ModelInput';
import styles from './DeploymentResult.module.css';

class DeploymentResult extends React.PureComponent {
  public render() {
    return (
      <Popup title="Successful deployment" isOpen={true} onRequestClose={console.log}>
        <div className={styles.deployment_result}>
          <div className={styles.commonInfo}>
            <Form>
              <Form.Item label="Model ID">22</Form.Item>
              <Form.Item label="Type">REST</Form.Item>
              <Form.Item label="URL">Copy</Form.Item>
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

export default DeploymentResult;
