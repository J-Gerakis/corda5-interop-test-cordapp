const { createProxyMiddleware } = require("http-proxy-middleware");

const ALICE_FRUIT_PORT = 12112;
const BOB_FRUIT_PORT = 12116;

module.exports = function (app) {
  app.use(
    "/alicefruit",
    createProxyMiddleware({
      target: `https://localhost:${ALICE_FRUIT_PORT}`,
      pathRewrite: { "^/alicefruit/api": "/api" },
      changeOrigin: true,
      secure: false,
    })
  );
  app.use(
    "/bobfruit",
    createProxyMiddleware({
      target: `https://localhost:${BOB_FRUIT_PORT}`,
      pathRewrite: { "^/bobfruit/api": "/api" },
      changeOrigin: true,
      secure: false,
    })
  );
};
