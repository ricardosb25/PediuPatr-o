const http = require('node:http');
const fs = require('node:fs/promises');
const path = require('node:path');
const raizProjeto = path.resolve(__dirname, '../../..');
const raizEstatica = path.join(raizProjeto, 'src/main/resources/static');
const raizPrevia = path.join(raizProjeto, 'target/dashboard-preview');
const tipos = {'.html': 'text/html; charset=utf-8', '.json': 'application/json; charset=utf-8', '.css': 'text/css; charset=utf-8', '.js': 'application/javascript; charset=utf-8', '.woff2': 'font/woff2', '.woff': 'font/woff', '.ttf': 'font/ttf', '.png': 'image/png', '.svg': 'image/svg+xml'};
const modoVazio = process.argv.includes('--vazio');
const modoErro = process.argv.includes('--erro');
const servidor = http.createServer(async function servirPrevia(requisicao, resposta) {
    try {
        const endereco = new URL(requisicao.url, 'http://127.0.0.1');
        if (requisicao.method !== 'GET') { resposta.writeHead(405); resposta.end(); return; }
        let arquivo;
        if (endereco.pathname === '/dashboard' || endereco.pathname === '/') arquivo = path.join(raizPrevia, 'dashboard.html');
        else if (endereco.pathname === '/api/dashboard/filtros') arquivo = path.join(raizPrevia, 'filtros.json');
        else if (endereco.pathname === '/api/dashboard/resumo') {
            if (modoErro) { resposta.writeHead(503, {'Content-Type': 'application/json'}); resposta.end(JSON.stringify({erro: 'Falha simulada exclusivamente na prévia de testes.'})); return; }
            arquivo = path.join(raizPrevia, modoVazio ? 'resumo-vazio.json' : 'resumo.json');
        } else {
            arquivo = path.resolve(raizEstatica, '.' + decodeURIComponent(endereco.pathname));
            if (!arquivo.startsWith(raizEstatica + path.sep)) { resposta.writeHead(403); resposta.end(); return; }
        }
        const conteudo = await fs.readFile(arquivo);
        resposta.writeHead(200, {'Content-Type': tipos[path.extname(arquivo)] || 'application/octet-stream', 'Cache-Control': 'no-store'});
        resposta.end(conteudo);
    } catch (falha) { resposta.writeHead(404); resposta.end('Arquivo de prévia não encontrado. Execute os testes primeiro.'); }
});
servidor.listen(18765, '127.0.0.1', function avisarPrevia() {
    console.log('Prévia com dados SINTÉTICOS de teste em http://127.0.0.1:18765/dashboard. Não usa MongoDB.');
});
