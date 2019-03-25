import * as React from 'react';
import { bind } from 'decko';

import Popup from 'components/shared/Popup/Popup';
import Tabs from 'components/shared/Tabs/Tabs';
import Button from 'components/shared/Button/Button';
import Select from 'components/shared/Select/Select';
import Checkbox from 'components/shared/Checkbox/Checkbox';
import FileUploader from 'components/shared/FileUploader/FileUploader';
import Form from 'components/shared/Form/Form';

import { DeployType } from 'models/Deploy';
import styles from './DeploySettings.module.css';

interface ILocalState {
  currentDeployType: DeployType;
  forms: {
    replicas: number;
    withLogs: boolean;
    withServiceMonitoring: boolean;
  };
}

class DeploySettings extends React.PureComponent<{}, ILocalState> {
  public state: ILocalState = {
    currentDeployType: 'rest',
    forms: {
      replicas: 2,
      withLogs: false,
      withServiceMonitoring: false
    }
  };

  public render() {
    const { currentDeployType, forms } = this.state;
    return (
      <Popup title="Deploy Confirmation" isOpen={true} onRequestClose={console.log}>
        <div className={styles.deploy_settings}>
          <Tabs<DeployType> active={currentDeployType} onSelectTab={this.onChangeDeployType}>
            <Tabs.Tab title="REST" type="rest" centered>
              <Form>
                <Form.Item label="Model ID">22</Form.Item>
                <Form.Item label="Replicas">
                  <Select<number>
                    value={forms.replicas}
                    options={[{ value: 3, label: '3' }, { value: 4, label: '4' }]}
                    onChange={this.onChangeRepliceCount}
                  />
                </Form.Item>
                <Form.Item label="Logs">
                  <Checkbox value={forms.withLogs} onChange={this.onToggleLogs} />
                </Form.Item>
                <Form.Item label="Service Monitoring">
                  <Checkbox value={forms.withServiceMonitoring} onChange={this.onToggleServiceMonitoring} />
                </Form.Item>
              </Form>
            </Tabs.Tab>
            <Tabs.Tab title="BATCH" type="batch" centered>
              <Form>
                <Form.Item label="Model ID">22</Form.Item>
                <Form.Item label="Replicas">
                  <Select<number>
                    value={forms.replicas}
                    options={[{ value: 3, label: '3' }, { value: 4, label: '4' }]}
                    onChange={this.onChangeRepliceCount}
                  />
                </Form.Item>
                <Form.Item label="Logs">
                  <Checkbox value={forms.withLogs} onChange={this.onToggleLogs} />
                </Form.Item>
                <Form.Item label="Service Monitoring">
                  <Checkbox value={forms.withServiceMonitoring} onChange={this.onToggleServiceMonitoring} />
                </Form.Item>
                <Form.Item label="Name for CSV file uploading?">
                  <FileUploader acceptFileTypes={['csv']} onUpload={console.log}>
                    {onSelectFile => (
                      <div className={styles.select_CSV_file_button} onClick={onSelectFile}>
                        Select .CSV
                      </div>
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
    this.setState({ currentDeployType: type });
  }

  @bind
  private onToggleLogs() {
    this.setState(prev => ({ forms: { ...prev.forms, withLogs: !this.state.forms.withLogs } }));
  }

  @bind
  private onToggleServiceMonitoring() {
    this.setState(prev => ({ forms: { ...prev.forms, withServiceMonitoring: !this.state.forms.withServiceMonitoring } }));
  }

  @bind
  private onChangeRepliceCount(value: number) {
    this.setState(prev => ({ forms: { ...prev.forms, replicas: value } }));
  }

  @bind
  private onDeploy() {
    console.log('deploy');
  }
}

export default DeploySettings;
