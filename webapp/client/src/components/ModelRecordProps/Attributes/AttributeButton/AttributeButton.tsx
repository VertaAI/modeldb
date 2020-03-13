import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { Attribute } from 'core/shared/models/Attribute';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';

import styles from './AttributeButton.module.css';

interface ILocalProps {
  attribute: Attribute;
  additionalClassname?: string;
  onButtonSize?(size: { height: number }): void;
}

interface ILocalState {
  isModalOpen: boolean;
}

class AttributeButton extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = {
    isModalOpen: false,
  };

  private buttonRefObject = React.createRef<HTMLDivElement>();

  public componentDidMount() {
    if (this.props.onButtonSize && this.buttonRefObject.current) {
      this.props.onButtonSize({
        height: this.buttonRefObject.current.offsetHeight,
      });
    }
  }

  public render() {
    const { attribute, additionalClassname } = this.props;
    const iconType = 'cube';

    return (
      <div>
        <Popup
          title="Attribute"
          titleIcon={iconType}
          contentLabel="attribute-action"
          isOpen={this.state.isModalOpen}
          onRequestClose={this.handleCloseModal}
        >
          <div className={styles.popupContent}>
            <div>
              <this.RenderField keystr="Key" value={attribute.key} />
              {typeof attribute.value === 'object' ? (
                Array.isArray(attribute.value) ? (
                  <this.RenderFieldList
                    keystr="Value"
                    value={attribute.value}
                  />
                ) : (
                  <this.RenderFieldObject
                    keystr="Value"
                    value={attribute.value}
                  />
                )
              ) : (
                <this.RenderField keystr="Value" value={attribute.value} />
              )}
            </div>
          </div>
        </Popup>

        <div
          className={cn(styles.attribute_link, styles.attribute_item)}
          ref={this.buttonRefObject}
          onClick={this.showModal}
        >
          <div
            className={cn(styles.attribute_wrapper, additionalClassname)}
            title="view attribute"
            data-test="attribute"
          >
            <div className={styles.notif}>
              <Icon className={styles.notif_icon} type={iconType} />
            </div>
            <div className={styles.attributeKey} data-test="attribute-key">
              {attribute.key}
            </div>
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

  private RenderField(props: { keystr?: string; value?: string | number }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value && <div className={styles.fieldValue}>{String(value)}</div>}
      </div>
    );
  }

  private RenderFieldList(props: {
    keystr?: string;
    value?: Array<string | number>;
  }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        <div className={styles.listBlock}>
          {value &&
            value.map(item => {
              return (
                <div className={styles.listFieldValue} key={String(item)}>
                  {String(item)}
                </div>
              );
            })}
        </div>
      </div>
    );
  }

  private RenderFieldObject(props: { keystr?: string; value?: Object }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        <div className={styles.valueContainer}>
          {value && (
            <div className={styles.objectFieldValue}>
              <pre>{JSON.stringify(value, null, 2)}</pre>
            </div>
          )}
        </div>
      </div>
    );
  }
}

export default AttributeButton;
