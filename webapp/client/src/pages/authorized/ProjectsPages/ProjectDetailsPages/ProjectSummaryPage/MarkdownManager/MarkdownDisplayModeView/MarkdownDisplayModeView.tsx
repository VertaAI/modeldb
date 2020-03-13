import { Markdown } from 'core/shared/utils/types';
import * as React from 'react';

import MarkdownIcon from '../shared/Icon/Icon';
import Layout from '../shared/Layout/Layout';
import MarkdownView from '../shared/Markdown/Markdown';
import TextWithIcon from '../shared/TextWithIcon/TextWithIcon';
import styles from './MarkdownDisplayModeView.module.css';

interface ILocalProps {
  title: string;
  value: Markdown;
  isEditDisabled: boolean;
  onEditMode(): void;
}

class MarkdownDisplayModeView extends React.PureComponent<ILocalProps> {
  public render() {
    const { title, value, isEditDisabled, onEditMode } = this.props;
    return (
      <Layout
        header={{
          leftContent: (
            <div className={styles.title}>
              <TextWithIcon icon="book" text={title} />
            </div>
          ),
          rightContent: !isEditDisabled && (
            <div className={styles.edit} onClick={onEditMode}>
              <MarkdownIcon type="share-write" />
            </div>
          ),
        }}
      >
        <MarkdownView value={value} />
      </Layout>
    );
  }
}

export default MarkdownDisplayModeView;
