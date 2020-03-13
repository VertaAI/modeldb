import { bind } from 'decko';
import * as React from 'react';

import { Markdown } from 'core/shared/utils/types';

import MarkdownDisplayModeView from './MarkdownDisplayModeView/MarkdownDisplayModeView';
import MarkdownEditorModeView from './MarkdownEditorModeView/MarkdownEditorModeView';

interface ILocalProps {
  title: string;
  initialValue: Markdown;
  isEditDisabled: boolean;
  onSaveChanges(value: Markdown): void;
}

interface ILocalState {
  mode: 'display' | 'edit';
}

type Mode = 'display' | 'edit';

class MarkdownManager extends React.PureComponent<ILocalProps, ILocalState> {
  public state: ILocalState = { mode: 'display' };

  public render() {
    const { title, initialValue, isEditDisabled } = this.props;
    const { mode } = this.state;
    return mode === 'display' ? (
      <MarkdownDisplayModeView
        title={title}
        value={initialValue}
        isEditDisabled={isEditDisabled}
        onEditMode={this.makeChangeMode('edit')}
      />
    ) : (
      <MarkdownEditorModeView
        initialValue={initialValue}
        onClose={this.makeChangeMode('display')}
        onSaveChanges={this.saveChanges}
      />
    );
  }

  @bind
  private makeChangeMode(mode: Mode) {
    return () => this.changeMode(mode);
  }
  @bind
  private changeMode(mode: Mode) {
    this.setState({ mode });
  }

  @bind
  private saveChanges(value: Markdown) {
    this.props.onSaveChanges(value);
    this.changeMode('display');
  }
}

export default MarkdownManager;
