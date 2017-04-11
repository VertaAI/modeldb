var assert = require('assert');
var thrift = require('thrift');
var types = require('../thrift/ModelDB_types.js');
var Service = require('../thrift/ModelDBService.js');
const argv = require('yargs')
    .usage("Usage: $0 [options]")
    .example("$0 --host backend")
    .default({
        'host': 'localhost',
        'port': '6543'
    })
    .help('help')
    .argv

var transport = thrift.TFramedTransport;
var protocol = thrift.TBinaryProtocol;

var connection = thrift.createConnection(argv.host, argv.port, {
  transport : transport,
  protocol : protocol
});

connection.on('error', function(err) {
  assert(false, err);
});

var client = thrift.createClient(Service, connection);

module.exports = {
  "client": client,
  "transport": transport
}
