const originalFetch = window.fetch.bind(window);
window.fetch = function fetchComCsrf(recurso, configuracao = {}) {
    const destino = new URL(recurso instanceof Request ? recurso.url : recurso, location.href);
    const metodo = (configuracao.method || (recurso instanceof Request ? recurso.method : 'GET')).toUpperCase();
    if (destino.origin === location.origin && !['GET', 'HEAD', 'OPTIONS'].includes(metodo)) {
        const cabecalhos = new Headers(configuracao.headers || (recurso instanceof Request ? recurso.headers : undefined));
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const nomeCabecalho = document.querySelector('meta[name="_csrf_header"]')?.content;
        if (token && nomeCabecalho) cabecalhos.set(nomeCabecalho, token);
        configuracao = { ...configuracao, headers: cabecalhos };
    }
    return originalFetch(recurso, configuracao);
};
