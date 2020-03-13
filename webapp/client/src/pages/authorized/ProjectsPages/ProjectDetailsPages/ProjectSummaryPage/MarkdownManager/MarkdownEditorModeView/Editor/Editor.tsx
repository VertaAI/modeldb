import * as codemirror from 'codemirror';
import 'codemirror-github-light/lib/codemirror-github-light-theme.css';
import 'codemirror/lib/codemirror.css';
import 'codemirror/mode/markdown/markdown';
import { bind } from 'decko';
import * as React from 'react';
import { Controlled as CodeMirror } from 'react-codemirror2';

import styles from './Editor.module.css';

interface ILocalProps {
  value: string;
  options: IOptions;
  onChange(value: string): void;
}

interface IOptions {
  isSoftLineWrap: boolean;
  isIndentWithTabs: boolean;
  indentSize: number;
}

class Editor extends React.PureComponent<ILocalProps> {
  public render() {
    const { value, options } = this.props;
    return (
      <div className={styles.root}>
        <CodeMirror
          value={value}
          options={{
            mode: 'markdown',
            theme: 'github-light',
            lineNumbers: true,
            lineWrapping: options.isSoftLineWrap,
            indentWithTabs: options.isIndentWithTabs,
            tabSize: options.indentSize,
          }}
          onChange={this.onChange}
          onBeforeChange={this.onChange}
        />
      </div>
    );
  }

  @bind
  private onChange(
    editor: codemirror.Editor,
    data: codemirror.EditorChange,
    value: string
  ) {
    this.props.onChange(value);
  }
}

export default Editor;
