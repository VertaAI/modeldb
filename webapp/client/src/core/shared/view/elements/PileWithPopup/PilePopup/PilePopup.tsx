import cn from 'classnames';
import * as React from 'react';

import Popup from 'core/shared/view/elements/Popup/Popup';

import styles from './PilePopup.module.css';

type IPopupLocalProps = GetReactComponentProps<Popup>;

type ILocalProps = IPopupLocalProps & {
  titleIcon: Required<IPopupLocalProps>['titleIcon'];
  additionalContentClassname?: string;
  children: any;
};

type GetReactComponentProps<T> = T extends React.Component<infer P> ? P : never;

class PilePopup extends React.PureComponent<ILocalProps> {
  public static Actions = Actions;
  public static Fields = Fields;
  public static SplittedContent = SplittedContent;

  public render() {
    return (
      <Popup {...this.props}>
        <div className={cn(styles.root, this.props.additionalContentClassname)}>
          {this.props.children}
        </div>
      </Popup>
    );
  }
}

function SplittedContent({
  left,
  right,
}: {
  left: React.ReactNode;
  right: React.ReactNode;
}) {
  return (
    <div className={styles.splittedContent}>
      <div className={styles.splittedContent_left}>{left}</div>
      <div className={styles.splittedContent_right}>{right}</div>
    </div>
  );
}

function Fields({
  children,
}: {
  children: (Field: typeof FieldComponent) => React.ReactNode;
}) {
  return <div className={styles.fields}>{children(FieldComponent)}</div>;
}
function FieldComponent({
  label,
  children,
  additionalContent,
}: {
  label: string;
  children: React.ReactNode;
  additionalContent?: React.ReactNode;
}) {
  return (
    <div className={styles.field}>
      <div className={styles.field__label}>{label}</div>
      <div className={styles.field__value}>{children}</div>
    </div>
  );
}

function Actions({ children }: { children: React.ReactNode }) {
  return <div className={styles.actions}>{children}</div>;
}

export default PilePopup;
