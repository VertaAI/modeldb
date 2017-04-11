var thrift = require('thrift');

const argv = require('yargs')
    .usage("Usage: $0 [options]")
    .example("$0 --host backend")
    .default({
        'host': 'localhost',
        'port': '6543'
    })
    .help('help')
    .argv

// Return 0 if able to make a Thrift connection to the given server, 1 otherwise.

var connection = thrift.createConnection(argv.host, argv.port, {
  transport : thrift.TFramedTransport,
  protocol : thrift.TBinaryProtocol
})
.on('error', (err) => process.exit(1))
.on('connect', () => process.exit(0))
.on('secureConnect', () => process.exit(0));
