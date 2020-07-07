import * as React from 'react';

import styles from './PilePopup.module.css';
import Dialog from '../../Dialog/Dialog';

type IPopupLocalProps = Omit<React.ComponentProps<typeof Dialog>, 'type'>;

type ILocalProps = IPopupLocalProps & {
  titleIcon: Required<IPopupLocalProps>['titleIcon'];
  children: JSX.Element;
};

class PilePopup extends React.PureComponent<ILocalProps> {
  public static Actions = Actions;
  public static Fields = Fields;
  public static Field = Field;

  public render() {
    return (
      <Dialog type="info" {...this.props}>
        <div className={styles.root}>{this.props.children}</div>
      </Dialog>
    );
  }
}

function Fields({ children }: { children: () => React.ReactNode }) {
  return <div className={styles.fields}>{children()}</div>;
}
function Field({
  label,
  children,
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
