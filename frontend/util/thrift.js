var assert = require('assert');
var thrift = require('thrift');
var types = require('../thrift/ModelDB_types.js');
var Service = require('../thrift/ModelDBService.js');

var transport = thrift.TFramedTransport;
var protocol = thrift.TBinaryProtocol;

var connection = thrift.createConnection("localhost", 6543, {
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