import { bind } from 'decko';
import * as React from 'react';

import ButtonLikeText from 'components/shared/ButtonLikeText/ButtonLikeText';
import CopyToClipboard from 'components/shared/CopyToClipboard/CopyToClipboard';
import Fab from 'components/shared/Fab/Fab';
import Form from 'components/shared/Form/Form';
import Popup from 'components/shared/Popup/Popup';
import { IDeployedStatusInfo } from 'models/Deploy';

import styles from './DeployResult.module.css';

interface ILocalProps {
  modelId: string;
  data: IDeployedStatusInfo['data'];
  onShutdown(): void;
  onClose(): void;
}

type AllProps = ILocalProps;

const mockApi = 'https://verta.io/234wfogsfas/fsfbgs';
const mockToken = '42';

class DeployResult extends React.Component<AllProps> {
  public render() {
    const {
      data: { api = mockApi, token = mockToken },
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
                  <ButtonLikeText to={api}>
                    <span className={styles.url}>{api}</span>
                  </ButtonLikeText>
                }
              >
                <CopyToClipboard text={api}>
                  {onCopy => (
                    <ButtonLikeText onClick={onCopy}>Copy</ButtonLikeText>
                  )}
                </CopyToClipboard>
              </Form.Item>
              <Form.Item
                label="Token"
                additionalContent={
                  <div className={styles.model_id}>{token}</div>
                }
              >
                <CopyToClipboard text={token}>
                  {onCopy => (
                    <ButtonLikeText onClick={onCopy}>Copy</ButtonLikeText>
                  )}
                </CopyToClipboard>
              </Form.Item>
            </Form>
          </div>
          <div className={styles.destroy}>
            <Fab theme="red" onClick={this.onShutdown}>
              Shutdown
            </Fab>
          </div>
          {/* <div className={styles.model_input}>
            <ModelInput input={modelApi.input} />
          </div> */}
        </div>
      </Popup>
    );
  }

  @bind
  private onShutdown() {
    this.props.onShutdown();
    this.props.onClose();
  }
}

export default DeployResult;
