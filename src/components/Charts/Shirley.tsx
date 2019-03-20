import React from 'react';
import BarChart from './sher/Bar';
import Chart from './sher/Chart';

class Shirley extends React.Component<{}, { city: string; temps: any }> {
  public constructor(props: any) {
    super(props);
    this.state = {
      city: 'sf',
      temps: {}
    };
  }

  public componentDidMount() {
    Promise.all([fetch(`${process.env.PUBLIC_URL || ''}/sf.json`), fetch(`${process.env.PUBLIC_URL || ''}/ny.json`)])
      .then(responses => Promise.all(responses.map((resp: any) => resp.json())))
      .then(([sf, ny]) => {
        sf.forEach((day: any) => (day.date = new Date(day.date)));
        ny.forEach((day: any) => (day.date = new Date(day.date)));
        this.setState({ temps: { sf, ny } });
      });
  }

  public updateCity = (event: React.FormEvent<HTMLSelectElement>) => {
    const element = event.target as HTMLSelectElement;
    this.setState({ city: element.value });
  };

  public render() {
    // console.log(this.state);
    const data = this.state.temps[this.state.city];
    // console.log(data);

    return (
      <div className="App">
        <p>
          2017 Temperatures for
          <select name="city" onChange={this.updateCity}>
            {[{ label: 'San Francisco', value: 'sf' }, { label: 'New York', value: 'ny' }].map(option => {
              return (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              );
            })}
          </select>
        </p>
        {console.log(data)}
        {/* <Chart data={data} /> */}
        {/* <BarChart data={data} /> */}
      </div>
    );
  }
}

export default Shirley;
