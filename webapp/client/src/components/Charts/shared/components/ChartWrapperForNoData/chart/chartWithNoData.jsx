import React, { Component } from 'react';
import { errorMessageOnLayer } from 'components/Charts/shared/errorMessages';

const width = 680;
const height = 360;
const messageMargin = 80;

class EmptyChart extends Component {
  componentDidMount() {
    const { errorMessage, canvasClassName } = this.props;
    errorMessageOnLayer(
      `.${canvasClassName}`,
      width,
      messageMargin,
      height,
      'notAvailableMsg',
      errorMessage,
      '\u20E0'
    );
  }

  _setRef(componentNode) {
    this._rootNode = componentNode;
  }

  render() {
    const { canvasClassName } = this.props;
    return (
      <div>
        <React.Fragment>
          <svg
            className={canvasClassName}
            width={width}
            height={height}
            ref={this._setRef.bind(this)}
          />
        </React.Fragment>
      </div>
    );
  }
}

export default EmptyChart;
