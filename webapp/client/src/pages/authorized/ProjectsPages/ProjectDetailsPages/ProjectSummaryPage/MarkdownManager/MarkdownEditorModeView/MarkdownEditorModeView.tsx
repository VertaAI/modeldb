import cn from 'classnames';
import { bind } from 'decko';
import * as React from 'react';

import { Markdown } from 'core/shared/utils/types';
import Confirm from 'core/shared/view/elements/Confirm/Confirm';
import Fab from 'core/shared/view/elements/Fab/Fab';
import { IconType } from 'core/shared/view/elements/Icon/Icon';

import MarkdownIcon from '../shared/Icon/Icon';
import Layout from '../shared/Layout/Layout';
import MarkdownView from '../shared/Markdown/Markdown';
import TextWithIcon from '../shared/TextWithIcon/TextWithIcon';
import Editor from './Editor/Editor';
import styles from './MarkdownEditorModeView.module.css';
import Select from './Select/Select';

interface ILocalProps {
  initialValue: Markdown;
  onSaveChanges(value: Markdown): void;
  onClose(): void;
}

interface ILocalState {
  mode: Mode;
  value: string;
  options: IOptions;
  isShowConfirmSavingOfChanges: boolean;
}

interface IOptions {
  lineWrapMode: LineWrapMode;
  indentMode: IndentMode;
  indentSize: number;
}

enum LineWrapMode {
  noWrap = 'noWrap',
  softWrap = 'softWrap',
}
enum IndentMode {
  spaces = 'spaces',
  tabs = 'tabs',
}

type Mode = 'edit' | 'preview';

class MarkdownEditorModeView extends React.PureComponent<
  ILocalProps,
  ILocalState
> {
  public state: ILocalState = {
    mode: 'edit',
    value: this.props.initialValue,
    options: {
      lineWrapMode: LineWrapMode.noWrap,
      indentMode: IndentMode.spaces,
      indentSize: 2,
    },
    isShowConfirmSavingOfChanges: false,
  };

  public render() {
    const { onClose } = this.props;
    const { mode, value, isShowConfirmSavingOfChanges, options } = this.state;
    return (
      <Layout
        header={{
          leftContent: (
            <div className={styles.modes}>
              <this.Mode icon="code" text="Edit" type="edit" />
              <this.Mode icon="share-read" text="Preview" type="preview" />
            </div>
          ),
          rightContent: (
            <div className={styles.actions}>
              {mode === 'edit' && (
                <div className={styles.options}>
                  <div className={styles.option}>
                    <Select<IndentMode>
                      title="Indent mode"
                      value={options.indentMode}
                      options={[
                        { label: 'Spaces', value: IndentMode.spaces },
                        { label: 'Tabs', value: IndentMode.tabs },
                      ]}
                      onChange={this.changeIndentMode}
                    />
                  </div>
                  <div className={styles.option}>
                    <Select<LineWrapMode>
                      title="Line wrap mode"
                      value={options.lineWrapMode}
                      options={[
                        { label: 'No wrap', value: LineWrapMode.noWrap },
                        { label: 'Soft wrap', value: LineWrapMode.softWrap },
                      ]}
                      onChange={this.changeLineWrapMode}
                    />
                  </div>
                  <div className={styles.option}>
                    <Select<number>
                      title="Line wrap mode"
                      value={options.indentSize}
                      options={[
                        { label: '2', value: 2 },
                        { label: '4', value: 4 },
                        { label: '8', value: 8 },
                      ]}
                      onChange={this.changeIndentSize}
                    />
                  </div>
                </div>
              )}
              <div className={styles.saveChanges}>
                <Confirm
                  title="Confirm"
                  isOpen={isShowConfirmSavingOfChanges}
                  onCancel={this.closeConfirmSavingOfChanges}
                  onConfirm={this.saveChanges}
                >
                  Are you sure?
                </Confirm>
                <Fab
                  theme="gray"
                  variant="outlined"
                  onClick={this.openConfirmSavingOfChanges}
                  size={'medium'}
                >
                  Save
                </Fab>
              </div>
              <div className={styles.close} onClick={onClose}>
                <MarkdownIcon type="close" />
              </div>
            </div>
          ),
        }}
      >
        {mode === 'edit' ? (
          <Editor
            value={value}
            options={{
              isSoftLineWrap: options.lineWrapMode === LineWrapMode.softWrap,
              isIndentWithTabs: options.indentMode === IndentMode.tabs,
              indentSize: options.indentSize,
            }}
            onChange={this.changeValue}
          />
        ) : (
          <MarkdownView value={value} />
        )}
      </Layout>
    );
  }

  @bind
  private Mode({
    type,
    icon,
    text,
  }: {
    type: Mode;
    icon: IconType;
    text: string;
  }) {
    return (
      <div
        className={cn(styles.mode, {
          [styles.activeMode]: this.state.mode === type,
        })}
        onClick={this.makeChangeMode(type)}
      >
        <TextWithIcon icon={icon} text={text} />
      </div>
    );
  }

  @bind
  private makeChangeMode(mode: Mode) {
    return () => this.changeMode(mode);
  }
  @bind
  private changeMode(mode: Mode) {
    return this.setState({ mode });
  }

  @bind
  private changeValue(value: Markdown) {
    this.setState({ value });
  }

  @bind
  private openConfirmSavingOfChanges() {
    this.setState({ isShowConfirmSavingOfChanges: true });
  }
  @bind
  private closeConfirmSavingOfChanges() {
    this.setState({ isShowConfirmSavingOfChanges: false });
  }

  @bind
  private saveChanges() {
    this.closeConfirmSavingOfChanges();
    this.props.onSaveChanges(this.state.value);
    this.props.onClose();
  }

  @bind
  private changeLineWrapMode(lineWrapMode: LineWrapMode) {
    this.setState({ options: { ...this.state.options, lineWrapMode } });
  }
  @bind
  private changeIndentMode(indentMode: IndentMode) {
    this.setState({ options: { ...this.state.options, indentMode } });
  }
  @bind
  private changeIndentSize(indentSize: number) {
    this.setState({ options: { ...this.state.options, indentSize } });
  }
}

export default MarkdownEditorModeView;
