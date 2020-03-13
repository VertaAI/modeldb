import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import {
  getDiffValueBorderClassname,
  getDiffValueBgClassname,
} from 'components/CompareEntities/shared/DiffHighlight/DiffHighlight';
import { IAttribute } from 'core/shared/models/Attribute';
import { Icon } from 'core/shared/view/elements/Icon/Icon';
import Popup from 'core/shared/view/elements/Popup/Popup';
import { EntityType } from 'store/compareEntities';

import { IComparedAttribute } from '../ComparableAttributes';
import styles from './ComparableAttributeButton.module.css';

interface ILocalProps {
  comparedAttribute: IComparedAttribute;
  entityType: EntityType;
}

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
      comparedAttribute: { diffInfo },
      comparedAttribute,
      entityType,
    } = this.props;
    const iconType = 'cube';

    const isDifferent = (() => {
      switch (diffInfo.type) {
        case 'singleAttribute':
          return true;
        case 'differentValueTypes':
          return true;
        case 'singleValueTypes':
          return diffInfo.isDifferent;
        case 'listValueTypes':
          return Object.values(diffInfo.diffInfo).some(f => f);
      }
    })();
    const buttonAdditionClassName = getDiffValueBorderClassname(
      entityType,
      isDifferent
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
            {(() => {
              if (entityType === EntityType.entity1) {
                return (
                  <>
                    <div className={styles.attribute}>
                      {this.renderAttribute(
                        comparedAttribute.attribute,
                        EntityType.entity1
                      )}
                    </div>
                    <div
                      className={cn(styles.attribute, {
                        [styles.empty]: !Boolean(
                          comparedAttribute.otherEntityAttribute
                        ),
                      })}
                    >
                      {comparedAttribute.otherEntityAttribute
                        ? this.renderAttribute(
                            comparedAttribute.otherEntityAttribute,
                            EntityType.entity2
                          )
                        : '-'}
                    </div>
                  </>
                );
              }
              return (
                <>
                  <div
                    className={cn(styles.attribute, {
                      [styles.empty]: !Boolean(
                        comparedAttribute.otherEntityAttribute
                      ),
                    })}
                  >
                    {comparedAttribute.otherEntityAttribute
                      ? this.renderAttribute(
                          comparedAttribute.otherEntityAttribute,
                          EntityType.entity1
                        )
                      : '-'}
                  </div>
                  <div className={styles.attribute}>
                    {this.renderAttribute(
                      comparedAttribute.attribute,
                      EntityType.entity2
                    )}
                  </div>
                </>
              );
            })()}
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
              {comparedAttribute.attribute.key}
            </div>
          </div>
        </div>
      </div>
    );
  }

  @bind
  private renderAttribute(attribute: IAttribute, entityType: EntityType) {
    const {
      comparedAttribute: { diffInfo },
    } = this.props;
    return (
      <div>
        <this.RenderField keystr="Key" value={attribute.key} />
        {typeof attribute.value === 'object' ? (
          Array.isArray(attribute.value) ? (
            <this.RenderFieldList
              keystr="Value"
              value={attribute.value}
              getAdditionalValueClassName={value => {
                const isDifferent = (() => {
                  if (
                    diffInfo.type === 'differentValueTypes' ||
                    diffInfo.type === 'singleAttribute' ||
                    diffInfo.type === 'singleValueTypes'
                  ) {
                    return true;
                  }
                  return diffInfo.diffInfo[value];
                })();
                return getDiffValueBgClassname(entityType, isDifferent);
              }}
            />
          ) : (
            <this.RenderFieldObject keystr="Value" value={attribute.value} />
          )
        ) : (
          <this.RenderField
            keystr="Value"
            value={attribute.value}
            additionalClassName={getDiffValueBgClassname(
              entityType,
              (() => {
                switch (diffInfo.type) {
                  case 'singleAttribute':
                    return true;
                  case 'singleValueTypes':
                    return diffInfo.isDifferent;
                  case 'differentValueTypes':
                    return true;
                  case 'listValueTypes':
                    return true;
                }
              })()
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
