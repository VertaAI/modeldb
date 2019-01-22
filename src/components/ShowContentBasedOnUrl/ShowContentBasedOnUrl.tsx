import * as React from 'react';

interface IOwnProps {
  path: string;
}

enum ContentType {
  IMAGE = 'image',
  OTHER = 'other'
}

export default class ShowContentBasedOnUrl extends React.Component<IOwnProps> {
  public state = {
    urlContentType: ContentType.OTHER
  };

  public componentDidMount() {
    const configInit: RequestInit = {
      cache: 'default',
      method: 'HEAD'
    };

    fetch(this.props.path, configInit).then((value: Response) => {
      const contentType = value.headers.get('content-type');
      if (!contentType) return;

      if (contentType.startsWith('image')) {
        this.setState({ ...this.state, urlContentType: ContentType.IMAGE });
      }
    });
  }

  public render() {
    let element = null;
    switch (this.state.urlContentType) {
      case ContentType.IMAGE: {
        element = <img width={200} height={100} src={this.props.path} />;
        break;
      }
      default: {
        element = <a href={this.props.path}>{this.props.path}</a>;
        break;
      }
    }

    return <div>{element}</div>;
  }
}
