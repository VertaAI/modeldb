import * as React from 'react';
import { bind } from 'decko';

import Popup from 'components/shared/Popup/Popup';
import Tabs from 'components/shared/Tabs/Tabs';
import Button from 'components/shared/Button/Button';
import Select from 'components/shared/Select/Select';
import Checkbox from 'components/shared/Checkbox/Checkbox';
import FileUploader from 'components/shared/FileUploader/FileUploader';

import { DeployType } from 'models/Deploy';
import styles from './DeploySettings.module.css';

interface ILocalState {
  currentDeployType: DeployType;
}

class DeploySettings extends React.PureComponent<{}, ILocalState> {
  public state: ILocalState = { currentDeployType: 'rest' };

  public render() {
    const { currentDeployType } = this.state;
    return (
      <Popup title="Deploy Confirmation" isOpen={true} onRequestClose={console.log}>
        <div className={styles.deploy_settings}>
          <Tabs<DeployType> active={currentDeployType} onSelectTab={this.onChangeDeployType}>
            <Tabs.Tab title="REST" type="rest" centered>
              <div className={styles.rows}>
                <this.Row title="model ID" content={22} />
                <this.Row
                  title="Replicas"
                  content={
                    <Select<number>
                      value={3}
                      options={[{ value: 3, label: '3' }, { value: 4, label: '4' }]}
                      onChange={this.onChangeRepliceCount}
                    />
                  }
                />
                <this.Row title="Logs" content={<Checkbox value={true} onChange={this.onToggleLogs} />} />
                <this.Row title="Service Monitoring" content={<Checkbox value={false} onChange={this.onToggleServiceMonitoring} />} />
              </div>
            </Tabs.Tab>
            <Tabs.Tab title="BATCH" type="batch" centered>
              <div className={styles.rows}>
                <this.Row title="Model ID" content={22} />
                <this.Row
                  title="Replicas"
                  content={
                    <Select<number>
                      value={3}
                      options={[{ value: 3, label: '3' }, { value: 4, label: '4' }]}
                      onChange={this.onChangeRepliceCount}
                    />
                  }
                />
                <this.Row title="Logs" content={<Checkbox value={true} onChange={this.onToggleLogs} />} />
                <this.Row title="Service Monitoring" content={<Checkbox value={false} onChange={this.onToggleServiceMonitoring} />} />
                <this.Row
                  title="Name for CSV file uploading?"
                  content={
                    <FileUploader acceptFileTypes={['csv']} onUpload={console.log}>
                      {onSelectFile => (
                        <div className={styles.select_CSV_file_button} onClick={onSelectFile}>
                          Select .CSV
                        </div>
                      )}
                    </FileUploader>
                  }
                />
              </div>
            </Tabs.Tab>
          </Tabs>
          <div className={styles.deploy_button}>
            <Button onClick={this.onDeploy}>launch deployment</Button>
          </div>
        </div>
      </Popup>
    );
  }

  // todo rename
  @bind
  private Row({ title, content }: { title: string; content: React.ReactChild }) {
    return (
      <div className={styles.row}>
        <div className={styles.row_title}>{title}</div>
        <div className={styles.row_content}>{content}</div>
      </div>
    );
  }

  @bind
  private onChangeDeployType(type: DeployType) {
    this.setState({ currentDeployType: type });
  }

  @bind
  private onToggleLogs() {
    console.log('toggle logs');
  }

  @bind
  private onToggleServiceMonitoring() {
    console.log('toggle service monitoring');
  }

  @bind
  private onChangeRepliceCount(value: number) {
    console.log('change replice count', value);
  }

  @bind
  private onDeploy() {
    console.log('deploy');
  }
}

export default DeploySettings;
