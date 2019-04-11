import * as React from 'react';

import styles from './ShowContentBasedOnUrl.module.css';

interface ILocalProps {
  path: string;
}

interface ILocalState {
  urlContentType: ContentType;
}

enum ContentType {
  IMAGE = 'image',
  OTHER = 'other',
}

export default class ShowContentBasedOnUrl extends React.Component<
  ILocalProps,
  ILocalState
> {
  public state: ILocalState = { urlContentType: ContentType.OTHER };

  public componentDidMount() {
    const configInit: RequestInit = {
      cache: 'default',
      method: 'HEAD',
    };

    if (this.props.path.split(':')[0] == 'http') {
      fetch(this.props.path, configInit).then((value: Response) => {
        const contentType = value.headers.get('content-type');
        if (!contentType) return;

        if (contentType.startsWith('image')) {
          this.setState({ ...this.state, urlContentType: ContentType.IMAGE });
        }
      });
    }
  }

  public render() {
    let element = null;
    switch (this.state.urlContentType) {
      case ContentType.IMAGE: {
        element = <img width={220} height={160} src={this.props.path} />;
        break;
      }
      default: {
        element = (
          <a className={styles.link} href={this.props.path}>
            {this.props.path}
          </a>
        );
        break;
      }
    }

    return <span>{element}</span>;
  }
}
