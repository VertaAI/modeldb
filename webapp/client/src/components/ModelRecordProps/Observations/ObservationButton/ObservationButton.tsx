import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { getFormattedDateTime } from 'core/shared/utils/formatters/dateTime';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';

import styles from './ObservationButton.module.css';
import ObservationsTable from './ObservationsTable';

interface IAttributeVal {
  timeStamp: Date;
  value: string;
}

interface ILocalProps {
  // IAttributeVal here do not let me iterate over the map
  groupedObs: Map<string, any>;
  attributeKey: string;
  additionalClassname?: string;
}

interface ILocalState {
  isModalOpen: boolean;
}

class AttributeButton extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
  };
  public render() {
    const { groupedObs, attributeKey, additionalClassname } = this.props;
    const iconType = 'binoculars-tilted';
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
            {groupedObs &&
              groupedObs !== undefined &&
              attributeKey &&
              attributeKey !== undefined && (
                <ObservationsTable
                  rows={groupedObs
                    .get(attributeKey)
                    .map((obs: IAttributeVal) => {
                      return {
                        ...obs,
                        timeStamp: getFormattedDateTime(obs.timeStamp),
                      };
                    })}
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
  private handleCloseModal() {
    this.setState({ isModalOpen: false });
  }

  @bind
  private showModal() {
    this.setState({ isModalOpen: true });
  }
}

export default AttributeButton;
