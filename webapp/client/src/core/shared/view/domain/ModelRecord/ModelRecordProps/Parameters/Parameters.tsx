import * as React from 'react';

import vertaDocLinks from 'core/shared/utils/globalConstants/vertaDocLinks';
import ModelRecord from 'models/ModelRecord';

import ClientSuggestion from '../shared/ClientSuggestion/ClientSuggestion';
import styles from './Parameters.module.css';

interface ILocalProps {
  prop: 'hyperparameters' | 'metrics';
  parameters: ModelRecord['hyperparameters'] | ModelRecord['metrics'];
  getValueClassname?(key: string): string | undefined;
}

class Parameters extends React.PureComponent<ILocalProps> {
  public render() {
    const {
      prop,
      parameters,
      getValueClassname = () => undefined,
    } = this.props;
    const paramDataTestPrefix =
      prop === 'hyperparameters' ? 'hyperparameter' : 'metric';
    return (
      <div className={styles.root} data-test={prop}>
        {parameters.length !== 0 ? (
          parameters.map(({ key, value }) => (
            <div
              className={styles.param}
              key={key}
              data-test={paramDataTestPrefix}
            >
              <div
                className={styles.param_key}
                title={key}
                data-test={`${paramDataTestPrefix}-key`}
              >
                {key}
              </div>
              <div className={styles.param_value} title={value as string}>
                <span
                  className={getValueClassname(key)}
                  data-test={`${paramDataTestPrefix}-value`}
                >
                  {value}
                </span>
              </div>
            </div>
          ))
        ) : prop === 'hyperparameters' ? (
          <ClientSuggestion
            fieldName={'hyperparameter'}
            clientMethod={'log_hyperparameters()'}
            link={vertaDocLinks.log_hyperparameters}
          />
        ) : (
          <ClientSuggestion
            fieldName={'metric'}
            clientMethod={'log_metric()'}
            link={vertaDocLinks.log_metric}
          />
        )}
      </div>
    );
  }
}

export default Parameters;
