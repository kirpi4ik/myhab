const portfinder = require('portfinder');

portfinder.basePort=3000
portfinder.getPortPromise().then((port) => { console.log(port) })