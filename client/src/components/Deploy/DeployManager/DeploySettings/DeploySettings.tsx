import { bind } from 'decko';
import * as React from 'react';

import Button from 'components/shared/Button/Button';
import Checkbox from 'components/shared/Checkbox/Checkbox';
import FileUploader from 'components/shared/FileUploader/FileUploader';
import Form from 'components/shared/Form/Form';
import Popup from 'components/shared/Popup/Popup';
import Select from 'components/shared/Select/Select';
import Tabs from 'components/shared/Tabs/Tabs';

import { DeployType, IDeployConfig } from 'models/Deploy';
import styles from './DeploySettings.module.css';

interface ILocalProps {
  modelId: string;
  onDeploy(config: IDeployConfig): void;
  onClose(): void;
}

interface ILocalState {
  deployType: DeployType;
  replicas: number;
  withLogs: boolean;
  withServiceMonitoring: boolean;
}

class DeploySettings extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    deployType: 'rest',
    replicas: 3,
    withLogs: false,
    withServiceMonitoring: false,
  };

  public render() {
    const { modelId, onClose } = this.props;
    const {
      deployType,
      replicas,
      withLogs,
      withServiceMonitoring,
    } = this.state;
    return (
      <Popup title="Deploy Confirmation" isOpen={true} onRequestClose={onClose}>
        <div className={styles.deploy_settings}>
          <Tabs<DeployType>
            active={deployType}
            onSelectTab={this.onChangeDeployType}
          >
            <Tabs.Tab title="REST" type="rest" centered={true}>
              <Form>
                <Form.Item label="Model ID">
                  <div className={styles.model_id}>{modelId}</div>
                </Form.Item>
                <Form.Item label="Replicas">
                  <Select
                    value={replicas}
                    options={[
                      { value: 3, label: '3' },
                      { value: 4, label: '4' },
                    ]}
                    onChange={this.onChangeRepliceCount}
                  />
                </Form.Item>
                <Form.Item label="Logs">
                  <Checkbox value={withLogs} onChange={this.onToggleLogs} />
                </Form.Item>
                <Form.Item label="Service Monitoring">
                  <Checkbox
                    value={withServiceMonitoring}
                    onChange={this.onToggleServiceMonitoring}
                  />
                </Form.Item>
              </Form>
            </Tabs.Tab>
            <Tabs.Tab title="BATCH" type="batch" centered={true}>
              <Form>
                <Form.Item label="Model ID">
                  <div className={styles.model_id}>{modelId}</div>
                </Form.Item>
                <Form.Item label="Replicas">
                  <Select
                    value={replicas}
                    options={[
                      { value: 3, label: '3' },
                      { value: 4, label: '4' },
                    ]}
                    onChange={this.onChangeRepliceCount}
                  />
                </Form.Item>
                <Form.Item label="Logs">
                  <Checkbox value={withLogs} onChange={this.onToggleLogs} />
                </Form.Item>
                <Form.Item label="Service Monitoring">
                  <Checkbox
                    value={withServiceMonitoring}
                    onChange={this.onToggleServiceMonitoring}
                  />
                </Form.Item>
                <Form.Item label="Name for CSV file uploading?">
                  <FileUploader
                    acceptFileTypes={['csv']}
                    onUpload={console.log}
                  >
                    {onSelectFile => (
                      <Button variant="like-link" onClick={onSelectFile}>
                        Select .CSV
                      </Button>
                    )}
                  </FileUploader>
                </Form.Item>
              </Form>
            </Tabs.Tab>
          </Tabs>
          <div className={styles.deploy_button}>
            <Button onClick={this.onDeploy}>launch deployment</Button>
          </div>
        </div>
      </Popup>
    );
  }

  @bind
  private onChangeDeployType(type: DeployType) {
    this.setState({ deployType: type });
  }

  @bind
  private onToggleLogs() {
    this.setState(prev => ({ withLogs: !prev.withLogs }));
  }

  @bind
  private onToggleServiceMonitoring() {
    this.setState(prev => ({
      withServiceMonitoring: !prev.withServiceMonitoring,
    }));
  }

  @bind
  private onChangeRepliceCount(value: number) {
    this.setState({ replicas: value });
  }

  @bind
  private onDeploy() {
    const {
      deployType,
      replicas,
      withLogs,
      withServiceMonitoring,
    } = this.state;
    const config: IDeployConfig = {
      type: deployType,
      replicas,
      withLogs,
      withServiceMonitoring,
    };
    this.props.onDeploy(config);
  }
}

export default DeploySettings;
