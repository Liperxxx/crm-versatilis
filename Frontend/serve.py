#!/usr/bin/env python3
"""Servidor HTTP estático com Content-Type correto e CORS para desenvolvimento."""
import http.server
import socketserver

PORT = 3000

class CORSHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header("Access-Control-Allow-Origin", "*")
        self.send_header("Cache-Control", "no-store, no-cache, must-revalidate")
        super().end_headers()

    def guess_type(self, path):
        mime, _ = super().guess_type(path)
        if path.endswith(".css"):
            return "text/css; charset=utf-8"
        if path.endswith(".js"):
            return "application/javascript; charset=utf-8"
        if path.endswith(".html"):
            return "text/html; charset=utf-8"
        return mime

    def log_message(self, format, *args):
        print(f"[{self.address_string()}] {format % args}")

with socketserver.TCPServer(("", PORT), CORSHandler) as httpd:
    print(f"Frontend disponível em: http://localhost:{PORT}/login.html")
    httpd.serve_forever()
