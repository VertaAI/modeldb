import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import {
  getDiffValueBorderClassname,
  getDiffValueBgClassname,
} from '../../../shared/DiffHighlight/DiffHighlight';
import { IAttribute } from 'shared/models/Attribute';
import { Icon } from 'shared/view/elements/Icon/Icon';
import Popup from 'shared/view/elements/Popup/Popup';
import { IAttributeDiff, checkAttributeIsDiff, IPrimitiveAttributeDiff, IListAttributeDiff } from '../../../../../store/compareModels/compareModels';

import styles from './ComparableAttributeButton.module.css';
import { PopupComparedEntities } from '../../../shared/PopupComparedEntities/PopupComparedEntities';

interface ILocalProps {
  currentAttribute: IModelAttributeByKey;
  modelsAttributesByKey: IModelsAttributesByKey;
}

export type IModelsAttributesByKey = Array<IModelAttributeByKey | undefined>; // todo rename
export type IModelAttributeByKey = { modelNumber: number; diff: IAttributeDiff; attribute: IAttribute; };

interface ILocalState {
  isModalOpen: boolean;
}

class ComparableAttributeButton extends React.PureComponent<
  ILocalProps,
  ILocalState
> {
  public state: ILocalState = {
    isModalOpen: false,
  };

  public render() {
    const {
      currentAttribute,
      modelsAttributesByKey,
    } = this.props;
    const iconType = 'cube';

    const buttonAdditionClassName = getDiffValueBorderClassname(
      currentAttribute.modelNumber,
      checkAttributeIsDiff(currentAttribute.diff),
    );

    return (
      <div>
        <Popup
          title="Comparing attributes"
          titleIcon={iconType}
          contentLabel="attribute-action"
          isOpen={this.state.isModalOpen}
          onRequestClose={this.handleCloseModal}
        >
          <div className={styles.popupContent}>
            <PopupComparedEntities entities={modelsAttributesByKey}>
              {(modelAttributesByKey) => (
                this.renderAttribute(modelAttributesByKey)
              )}
            </PopupComparedEntities>
          </div>
        </Popup>

        <div
          className={cn(styles.attribute_link, styles.attribute_item)}
          onClick={this.showModal}
        >
          <div
            className={cn(styles.attribute_wrapper, buttonAdditionClassName)}
            title="view attribute"
            data-test="attribute"
          >
            <div className={styles.notif}>
              <Icon className={styles.notif_icon} type={iconType} />
            </div>
            <div className={styles.attributeKey} data-test="attribute-key">
              {currentAttribute.attribute.key}
            </div>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private renderAttribute({ modelNumber, attribute, diff }: IModelAttributeByKey) {
    return (
      <div>
        <this.RenderField keystr="Key" value={attribute.key} />
        {typeof attribute.value === 'object' ? (
          Array.isArray(attribute.value) ? (
            <this.RenderFieldList
              keystr="Value"
              value={attribute.value}
              getAdditionalValueClassName={value =>
                getDiffValueBgClassname(modelNumber, (diff as IListAttributeDiff).diffInfo[value])
              }
            />
          ) : (
            <this.RenderFieldObject keystr="Value" value={attribute.value} />
          )
        ) : (
          <this.RenderField
            keystr="Value"
            value={attribute.value}
            additionalClassName={getDiffValueBgClassname(
              modelNumber,
              (diff as IPrimitiveAttributeDiff).isDiff
            )}
          />
        )}
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

  private RenderField(props: {
    keystr?: string;
    value?: string | number;
    additionalClassName?: string;
  }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        {value ? (
          <div className={cn(styles.fieldValue, props.additionalClassName)}>
            {String(value)}
          </div>
        ) : (
          '-'
        )}
      </div>
    );
  }

  private RenderFieldList(props: {
    keystr?: string;
    value?: Array<string | number>;
    getAdditionalValueClassName: (value: string | number) => string | undefined;
  }) {
    const { keystr, value, getAdditionalValueClassName } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        <div className={styles.listBlock}>
          {value && value.length > 0
            ? value.map(item => {
                return (
                  <div
                    className={cn(
                      styles.listFieldValue,
                      getAdditionalValueClassName(item)
                    )}
                    key={String(item)}
                  >
                    {String(item)}
                  </div>
                );
              })
            : '-'}
        </div>
      </div>
    );
  }

  private RenderFieldObject(props: { keystr?: string; value?: object }) {
    const { keystr, value } = props;
    return (
      <div className={styles.popupField}>
        <div className={styles.fieldKey}>{keystr}</div>
        <div className={styles.valueContainer}>
          {value ? (
            <div className={styles.objectFieldValue}>
              <pre>{JSON.stringify(value, null, 2)}</pre>
            </div>
          ) : (
            '-'
          )}
        </div>
      </div>
    );
  }
}

export default ComparableAttributeButton;
