module.exports = function (req, res, next) {
  const db = req.app.db

  // helpdesk-commands mock: queued refund and forced refund
  if (req.method === 'POST' && /^(\/v1)?\/commands\/transactions\/[^/]+\/refund$/.test(req.path)) {
    return res.sendStatus(202);
  }
  if (req.method === 'POST' && /^(\/v1)?\/commands\/refund$/.test(req.path)) {
    return res.status(200).json({ refundOperationId: `R${Date.now()}` });
  }

  if (req.method === 'POST') {
    // Converts POST to GET
    req.method = 'GET'
  }
  const requestPath = req.path.toString();
  const requestBody = req.body;
  console.log(`${new Date().toISOString()} - Received request: ${req.method} ${req.path}
     Headers:
     ${JSON.stringify(req.headers)}
     Path: ${requestPath}
     Body:
     ${JSON.stringify(req.body)}
     `);

  next();
}