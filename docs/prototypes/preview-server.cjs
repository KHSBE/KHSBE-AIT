const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const allowed = new Set(['design-directions.html','directions.css','directions-variants.css','directions.js','itinerary-preview.html']);
const mime = {'.html':'text/html; charset=utf-8','.css':'text/css; charset=utf-8','.js':'text/javascript; charset=utf-8'};
http.createServer((req,res)=>{
 const name = new URL(req.url,'http://localhost').pathname.slice(1) || 'design-directions.html';
 if(!allowed.has(name)){res.writeHead(404);res.end('Not found');return;}
 res.writeHead(200,{'Content-Type':mime[path.extname(name)],'Cache-Control':'no-store'});
 fs.createReadStream(path.join(__dirname,name)).pipe(res);
}).listen(4179,'127.0.0.1',()=>console.log('Design preview: http://127.0.0.1:4179/'));