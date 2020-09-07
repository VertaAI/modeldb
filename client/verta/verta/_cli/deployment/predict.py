# -*- coding: utf-8 -*-

import click
import json

from .deployment import deployment
from ... import Client


@deployment.group()
def predict():
    """Making prediction to a deployment-related entity.

    For example, to make a prediction to an endpoint, run

    `verta deployment predict endpoint "<endpoint path>" --data "<input data>"`

    """
    pass


@predict.command(name="endpoint")
@click.argument("path", nargs=1, required=True)
@click.option("--data", "-d", required=True, help="Input for prediction. Must be a valid JSON string.")
@click.option("--workspace", "-w", help="Workspace to use.")
def predict_endpoint(path, data, workspace):
    """Making prediction via a deployed endpoint.
    """
    client = Client()

    try:
        endpoint = client.get_endpoint(path=path, workspace=workspace)
    except ValueError:
        raise click.BadParameter("endpoint with path {} not found".format(path))

    deployed_model = endpoint.get_deployed_model()
    result = deployed_model.predict(json.loads(data))
    click.echo(json.dumps(result))
