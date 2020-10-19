import cn from 'classnames';
import * as React from 'react';

import { Icon } from '../Icon/Icon';
import styles from './PageComponents.module.css';

export const PageCard = ({
  children,
  dataTest,
  additionalClassname,
}: {
  children: React.ReactNode;
  dataTest?: string;
  additionalClassname?: string;
}) => {
  return (
    <div
      className={cn(styles.pageCard, additionalClassname)}
      data-test={dataTest}
    >
      {children}
    </div>
  );
};

export const PageHeader = React.memo(
  ({
    title,
    rightContent,
    size = 'medium',
    withoutSeparator,
  }: {
    title: Exclude<React.ReactNode, null | undefined>;
    rightContent?: React.ReactNode;
    size?: 'medium' | 'small';
    withoutSeparator?: boolean;
  }) => (
    <div
      className={cn(styles.pageHeader, {
        [styles.pageHeader_withoutSeparator]: withoutSeparator,
        [styles.pageHeader_size_medium]: size === 'medium',
        [styles.pageHeader_size_small]: size === 'small',
      })}
    >
      <div
        className={cn(styles.pageHeader__title)}
        data-type="page-header-title"
      >
        {title}
      </div>
      {rightContent && (
        <div className={styles.pageHeader__rightContent}>{rightContent}</div>
      )}
    </div>
  )
);

export const PageContent = ({
  children,
  additionalClassname,
}: {
  children: React.ReactNode;
  additionalClassname?: string;
}) => (
  <div className={cn(styles.pageContent, additionalClassname)}>{children}</div>
);

export const RecordInfo = React.memo(
  ({
    label,
    children,
    additionalValueClassname,
    additionalRootClassname,
    dataTests = { root: 'record', label: undefined, value: undefined },
  }: {
    label: string;
    children: React.ReactNode;
    additionalValueClassname?: string;
    additionalRootClassname?: string;
    dataTests?: {
      root: string;
      label?: string;
      value?: string;
    };
  }) => (
    <div
      className={cn(styles.record, additionalRootClassname)}
      data-test={dataTests.root}
    >
      <span
        className={styles.record__label}
        data-test={dataTests.label || `${dataTests.root}-label`}
      >
        {label}
      </span>
      <span
        className={cn(styles.record__value, additionalValueClassname)}
        data-test={dataTests.value || `${dataTests.root}-value`}
      >
        {children}
      </span>
    </div>
  )
);

export const NA = '-';

export const PageSection = React.memo(
  (_: { title: string; children: React.ReactNode }) => {
    return null;
  }
);

export const PageCardWithNotificationView = ({
  notifaction,
  ...pageCardProps
}: React.ComponentProps<typeof PageCard> & {
  notifaction: React.ReactNode;
}) => {
  return (
    <PageCard
      {...pageCardProps}
      additionalClassname={cn(
        pageCardProps.additionalClassname,
        styles.pageCardWithNotification,
        {
          [styles.showNotification]: Boolean(notifaction),
        }
      )}
    >
      {notifaction}
      {pageCardProps.children}
    </PageCard>
  );
};

export const PageCardWithNotification = ({
  notifaction,
  ...pageCardProps
}: React.ComponentProps<typeof PageCard> & {
  notifaction: React.ReactNode;
}) => {
  return (
    <PageCard
      {...pageCardProps}
      additionalClassname={cn(
        pageCardProps.additionalClassname,
        styles.pageCardWithNotification,
        {
          [styles.showNotification]: Boolean(notifaction),
        }
      )}
    >
      {notifaction}
      {pageCardProps.children}
    </PageCard>
  );
};

export const Notification = ({
  children,
  type,
  dataTest,
  onClose,
}: {
  children: Exclude<React.ReactNode, null | undefined>;
  type: 'success' | 'failure';
  dataTest?: string;
  onClose(): void;
}) => {
  return (
    <div
      className={cn(styles.notification, {
        [styles.notification_success]: type === 'success',
        [styles.notification_failure]: type === 'failure',
      })}
      data-test={dataTest}
    >
      {children}
      <Icon
        type="close"
        className={styles.notification__close}
        onClick={onClose}
      />
    </div>
  );
};
