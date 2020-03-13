import * as React from 'react';
import ReactMarkdown from 'react-markdown';

import { Markdown } from 'core/shared/utils/types';

import 'github-markdown-css';
import './Markdown.module.css';

interface ILocalProps {
  value: Markdown;
}

class MarkdownView extends React.PureComponent<ILocalProps> {
  public render() {
    return (
      <ReactMarkdown className={'markdown-body'} source={this.props.value} />
    );
  }
}

export default MarkdownView;
