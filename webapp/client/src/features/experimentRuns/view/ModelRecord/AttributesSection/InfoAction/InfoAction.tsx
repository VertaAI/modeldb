import * as React from 'react';

import { Action } from 'shared/view/elements/PileWithActions/PileWithActions';
import PilePopup from 'shared/view/elements/PileWithActions/PipePopup/PipePopup';
import { IconType } from 'shared/view/elements/Icon/Icon';
import { IAttribute } from 'shared/models/Attribute';

import styles from './InfoAction.module.css';

export const useInfoAction = ({
  popupIconType,
  attribute,
}: {
  popupIconType: IconType;
  attribute: IAttribute;
}) => {
  const [isShowPopupInfo, changeIsShowPopupInfo] = React.useState(false);

  return {
    content: (
      <>
        {isShowPopupInfo && (
          <InfoPopup
            onClose={() => changeIsShowPopupInfo(false)}
            attribute={attribute}
            iconType={popupIconType}
          />
        )}
        <Action
          iconType="preview"
          onClick={() => changeIsShowPopupInfo(true)}
        />
      </>
    ),
  };
};

const InfoPopup = ({
  attribute,
  iconType,
  onClose,
}: {
  iconType: IconType;
  attribute: IAttribute;
  onClose: () => void;
}) => {
  return (
    <PilePopup
      title={`Information for ${attribute.key}`}
      isOpen={true}
      titleIcon={iconType}
      onRequestClose={onClose}
    >
      <PilePopup.Fields>
        {() => (
          <>
            <PilePopup.Field label="Key">{attribute.key}</PilePopup.Field>
            {typeof attribute.value === 'object' ? (
              Array.isArray(attribute.value) ? (
                <FieldList label="Value">{attribute.value}</FieldList>
              ) : (
                <FieldObject label="Value">{attribute.value}</FieldObject>
              )
            ) : (
              <PilePopup.Field label="Value">{attribute.value}</PilePopup.Field>
            )}
          </>
        )}
      </PilePopup.Fields>
    </PilePopup>
  );
};

const FieldList = ({
  label,
  children,
}: {
  label: string;
  children: Array<string | number>;
}) => {
  return (
    <PilePopup.Field label={label}>
      <div className={styles.listBlock}>
        {children.map((item) => {
          return (
            <div className={styles.listFieldValue} key={String(item)}>
              {String(item)}
            </div>
          );
        })}
      </div>
    </PilePopup.Field>
  );
};

const FieldObject = ({
  label,
  children,
}: {
  label: string;
  children: Object;
}) => {
  return (
    <PilePopup.Field label={label}>
      <div className={styles.valueContainer}>
        {
          <div className={styles.objectFieldValue}>
            <pre>{JSON.stringify(children, null, 2)}</pre>
          </div>
        }
      </div>
    </PilePopup.Field>
  );
};
