import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { getFormattedDateTime } from 'shared/utils/formatters/dateTime';
import { Icon } from 'shared/view/elements/Icon/Icon';
import Popup from 'shared/view/elements/Popup/Popup';
import Table from 'shared/view/elements/Table/Table';
import { IObservationsValues } from 'shared/models/Observation';

import styles from './ObservationButton.module.css';

interface ILocalProps {
  attributeKey: string;
  values: IObservationsValues[];
  additionalClassname?: string;
}

interface ILocalState {
  isModalOpen: boolean;
}

class ObservationButton extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
  };
  public render() {
    const { attributeKey, values, additionalClassname } = this.props;
    const iconType = 'binoculars-tilted';
    const epochColumn = {
      type: 'epochNumber',
      title: 'Epoch number',
      width: '33%',
      render: ({ epochNumber }: { epochNumber: string | number }) => (
        <span>{epochNumber}</span>
      ),
    };
    return (
      <div>
        <Popup
          title={`Observations - ${attributeKey}`}
          titleIcon={iconType}
          contentLabel="observations-action"
          isOpen={this.state.isModalOpen}
          onRequestClose={this.handleCloseModal}
        >
          <div className={styles.popupContent}>
            {attributeKey && (
              <Table
                dataRows={values.map(obs => {
                  return {
                    ...obs,
                    epochNumber:
                      obs.epochNumber === undefined ? '-' : obs.epochNumber,
                    timeStamp: getFormattedDateTime(obs.timeStamp),
                  };
                })}
                getRowKey={this.getRowKey}
                columnDefinitions={(this.isWithEpoch(values)
                  ? [epochColumn]
                  : []
                ).concat([
                  {
                    type: 'timeStamp',
                    title: 'TimeStamp',
                    width: '33%',
                    render: ({ timeStamp }: any) => <span>{timeStamp}</span>,
                  },
                  {
                    type: 'value',
                    title: 'Value',
                    width: '34%',
                    render: ({ value }: any) => <span>{value}</span>,
                  },
                ])}
              />
            )}
          </div>
        </Popup>

        <div
          className={cn(styles.attribute_link, styles.attribute_item)}
          onClick={this.showModal}
        >
          <div
            className={cn(styles.attribute_wrapper, additionalClassname)}
            title="view observations"
          >
            <div className={styles.notif}>
              <Icon className={styles.notif_icon} type={iconType} />
            </div>
            <div className={styles.attributeKey}>{attributeKey}</div>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private isWithEpoch(values: IObservationsValues[]) {
    return values.some(d => d.epochNumber !== undefined);
  }

  @bind
  private getRowKey(row: { timeStamp: string; epochNumber: string | number }) {
    return row.timeStamp;
  }

  @bind
  private handleCloseModal() {
    this.setState({ isModalOpen: false });
  }

  @bind
  private showModal() {
    this.setState({ isModalOpen: true });
  }
}

export default ObservationButton;
