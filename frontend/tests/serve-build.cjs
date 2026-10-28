const http = require("node:http");
const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..", "dist");
const mime = { ".html": "text/html", ".js": "application/javascript", ".css": "text/css",
  ".png": "image/png", ".avif": "image/avif" };
const server = http.createServer((request, response) => {
  const pathname = new URL(request.url, "http://127.0.0.1").pathname;
  if (pathname === "/__test_stop") {
    response.end("Test server stopped");
    server.close();
    return;
  }
  const file = path.resolve(root, "." + (path.extname(pathname) ? pathname : "/index.html"));
  if (!file.startsWith(root + path.sep)) {
    response.writeHead(403); response.end("Forbidden"); return;
  }
  fs.readFile(file, (error, content) => {
    if (error) {
      console.error("Test server read failed:", error.code, file);
      response.writeHead(404); response.end("Not found"); return;
    }
    response.setHeader("Content-Type", mime[path.extname(file)] || "application/octet-stream");
    response.end(content);
  });
});
server.listen(4175, "127.0.0.1", () => console.log("Offline shell test: http://127.0.0.1:4175"));
setTimeout(() => server.close(), 180000).unref();
