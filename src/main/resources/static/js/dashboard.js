(() => {
    'use strict';

    const moeda = new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'});
    const inteiro = new Intl.NumberFormat('pt-BR', {maximumFractionDigits: 0});
    const percentual = new Intl.NumberFormat('pt-BR', {minimumFractionDigits: 1, maximumFractionDigits: 2});
    const estado = {configuracao: null, filtrosAplicados: null, controlador: null, versao: 0, carregando: false, possuiDados: false, graficoVendas: null, graficoStatus: null, intervalo: null};
    const coresStatus = {RECEBIDO: '#adb5bd', EM_PREPARACAO: '#e5a322', PRONTO: '#17a2b8', SAIU_PARA_ENTREGA: '#007bff', RETIRADO: '#6554a4', FINALIZADO: '#328354', CANCELADO: '#c94c56', NAO_INFORMADO: '#6c757d'};
    const formulario = document.getElementById('filtros-dashboard');
    const inicioCampo = document.getElementById('inicio');
    const fimCampo = document.getElementById('fim');
    const statusCampo = document.getElementById('status');

    function texto(identificador, valor) {
        document.getElementById(identificador).textContent = valor;
    }

    function elemento(tag, conteudo, classe) {
        const componente = document.createElement(tag);
        if (conteudo !== undefined && conteudo !== null) componente.textContent = conteudo;
        if (classe) componente.className = classe;
        return componente;
    }

    function dataCurta(data) {
        return new Intl.DateTimeFormat('pt-BR', {day: '2-digit', month: '2-digit', timeZone: 'UTC'}).format(new Date(data + 'T12:00:00Z'));
    }

    function dataCompleta(data) {
        return new Intl.DateTimeFormat('pt-BR', {day: '2-digit', month: '2-digit', year: 'numeric', timeZone: 'UTC'}).format(new Date(data + 'T12:00:00Z'));
    }

    function instanteLocal(instante) {
        return new Intl.DateTimeFormat('pt-BR', {day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit', timeZone: estado.configuracao.fusoHorario}).format(new Date(instante));
    }

    function rotuloStatus(status) {
        return estado.configuracao.status.find(opcao => opcao.valor === status)?.rotulo || 'Não informado';
    }

    async function buscarJson(caminho, sinal) {
        let resposta;
        try {
            resposta = await fetch(caminho, {signal: sinal, credentials: 'same-origin', headers: {Accept: 'application/json'}, cache: 'no-store'});
        } catch (falha) {
            if (falha.name === 'AbortError') throw falha;
            throw new Error('Falha de conexão. Verifique sua rede e tente atualizar novamente.');
        }
        if (resposta.redirected || resposta.status === 401 || resposta.headers.get('content-type')?.includes('text/html')) {
            throw Object.assign(new Error('Sua sessão expirou. Entre novamente para consultar o painel.'), {status: 401});
        }
        const conteudo = await resposta.json().catch(() => null);
        if (!resposta.ok) {
            const mensagem = resposta.status === 403 ? 'Seu perfil não tem acesso a este painel.' : conteudo?.erro || 'Não foi possível consultar os dados. Tente novamente.';
            throw Object.assign(new Error(mensagem), {status: resposta.status});
        }
        if (!conteudo) throw new Error('A API retornou uma resposta inválida. Tente novamente.');
        return conteudo;
    }

    function mostrarErro(falha) {
        texto('mensagem-erro', falha.message || 'Falha de conexão. Verifique sua rede e tente novamente.');
        document.getElementById('erro-dashboard').hidden = false;
        document.getElementById('acesso-login').hidden = falha.status !== 401;
        if (estado.possuiDados) texto('estado-consulta', 'Exibindo a última consulta');
        if (falha.status === 401 || falha.status === 403) document.getElementById('atualizacao-automatica').checked = false;
    }

    function marcarCarregamento(ativo) {
        estado.carregando = ativo;
        document.getElementById('aplicar-filtros').disabled = ativo;
        document.getElementById('atualizar-dashboard').disabled = ativo;
        document.getElementById('dados-dashboard').setAttribute('aria-busy', String(ativo));
        document.getElementById('carregamento-dashboard').hidden = !ativo || estado.possuiDados;
    }

    async function atualizarDashboard(filtros = estado.filtrosAplicados) {
        if (!filtros) return;
        estado.controlador?.abort();
        const controlador = new AbortController();
        estado.controlador = controlador;
        const versao = ++estado.versao;
        const limite = setTimeout(() => controlador.abort(), 15000);
        marcarCarregamento(true);
        document.getElementById('erro-dashboard').hidden = true;
        try {
            const parametros = new URLSearchParams({inicio: filtros.inicio, fim: filtros.fim});
            if (filtros.status) parametros.set('status', filtros.status);
            const dados = await buscarJson('/api/dashboard/resumo?' + parametros, controlador.signal);
            if (versao !== estado.versao) return;
            renderizarResumo(dados);
            estado.filtrosAplicados = {inicio: dados.filtro.inicio, fim: dados.filtro.fim, status: dados.filtro.status || ''};
            const endereco = new URL(location.href);
            endereco.search = parametros.toString();
            history.replaceState(null, '', endereco);
        } catch (falha) {
            if (versao !== estado.versao) return;
            mostrarErro(falha.name === 'AbortError' ? new Error('A consulta demorou mais de 15 segundos. Tente atualizar novamente.') : falha);
        } finally {
            clearTimeout(limite);
            if (versao === estado.versao) marcarCarregamento(false);
        }
    }

    function renderizarResumo(dados) {
        const indicadores = dados.indicadores;
        texto('kpi-faturamento', moeda.format(indicadores.faturamentoLiquido));
        texto('kpi-pedidos', inteiro.format(indicadores.totalPedidos));
        texto('kpi-ticket', moeda.format(indicadores.ticketMedio));
        texto('kpi-andamento', inteiro.format(indicadores.pedidosEmAndamento));
        texto('kpi-finalizados', inteiro.format(indicadores.pedidosFinalizados) + ' finalizados');
        texto('kpi-descontos', moeda.format(indicadores.descontosConcedidos) + ' em descontos nas vendas');
        texto('kpi-cancelamentos', inteiro.format(indicadores.pedidosCancelados) + ' cancelados · ' + percentual.format(indicadores.taxaCancelamento) + '% do total');
        texto('periodo-aplicado', dataCompleta(dados.filtro.inicio) + ' — ' + dataCompleta(dados.filtro.fim) + ' / ' + (dados.filtro.status ? rotuloStatus(dados.filtro.status) : 'Todos os status'));
        texto('ultima-atualizacao', 'Atualizado em ' + instanteLocal(dados.atualizadoEm));
        texto('estado-consulta', '');
        document.getElementById('periodo-vazio').hidden = indicadores.totalPedidos !== 0;
        const avisos = document.getElementById('avisos-dashboard');
        avisos.replaceChildren(...dados.avisos.map(aviso => elemento('div', aviso)));
        avisos.hidden = dados.avisos.length === 0;
        renderizarVendas(dados.vendasPorDia);
        renderizarStatus(dados.situacaoPedidos);
        renderizarRanking(dados.produtosMaisVendidos);
        renderizarRecentes(dados.pedidosRecentes);
        estado.possuiDados = true;
        document.getElementById('dados-dashboard').hidden = false;
        if (typeof Chart === 'function') {
            desenharGraficos(dados);
        } else {
            avisos.appendChild(elemento('div', 'Os gráficos não carregaram. Os valores estão disponíveis nas tabelas e listas.'));
            avisos.hidden = false;
        }
    }

    function renderizarVendas(vendas) {
        const corpo = document.getElementById('tabela-vendas');
        corpo.replaceChildren(...vendas.map(venda => {
            const linha = elemento('tr');
            linha.append(elemento('td', dataCompleta(venda.data)), elemento('td', inteiro.format(venda.pedidos)), elemento('td', moeda.format(venda.faturamentoLiquido), 'text-right'));
            return linha;
        }));
    }

    function renderizarStatus(situacoes) {
        const lista = document.getElementById('lista-status');
        lista.replaceChildren(...situacoes.map(situacao => {
            const linha = elemento('li');
            const marcador = elemento('i', null, 'key-dot');
            marcador.setAttribute('aria-hidden', 'true');
            marcador.style.backgroundColor = coresStatus[situacao.status] || coresStatus.NAO_INFORMADO;
            linha.append(marcador, elemento('span', rotuloStatus(situacao.status)), elemento('strong', inteiro.format(situacao.quantidade)));
            return linha;
        }));
    }

    function renderizarRanking(produtos) {
        const corpo = document.getElementById('ranking-produtos');
        if (!produtos.length) {
            const linha = elemento('tr');
            const mensagem = elemento('td', 'Nenhum produto vendido em pedidos finalizados.', 'text-muted text-center py-4');
            mensagem.colSpan = 4;
            linha.appendChild(mensagem);
            corpo.replaceChildren(linha);
            return;
        }
        corpo.replaceChildren(...produtos.map((produto, posicao) => {
            const linha = elemento('tr');
            linha.append(
                elemento('td', posicao + 1),
                elemento('td', produto.nome),
                elemento('td', inteiro.format(produto.quantidade), 'text-right'),
                elemento('td', moeda.format(produto.valorBruto), 'text-right')
            );
            return linha;
        }));
    }

    function renderizarRecentes(pedidos) {
        const corpo = document.getElementById('pedidos-recentes');
        if (!pedidos.length) {
            const linha = elemento('tr');
            const mensagem = elemento('td', 'Nenhum pedido no período.', 'text-muted text-center py-4');
            mensagem.colSpan = 4;
            linha.appendChild(mensagem);
            corpo.replaceChildren(linha);
            return;
        }

        corpo.replaceChildren(...pedidos.map(pedido => {
            const linha = elemento('tr');
            const codigo = elemento('td', '#' + pedido.id.slice(-8).toUpperCase());
            codigo.title = pedido.id;
            const entrada = elemento('td');
            const horario = elemento('time', instanteLocal(pedido.dataEntrada));
            horario.dateTime = pedido.dataEntrada;
            entrada.appendChild(horario);
            const total = pedido.valorTotal === null ? '—' : moeda.format(pedido.valorTotal);
            linha.append(codigo, entrada, elemento('td', rotuloStatus(pedido.status)), elemento('td', total, 'text-right'));
            return linha;
        }));
    }

    function desenharGraficos(dados) {
        const movimentoReduzido = matchMedia('(prefers-reduced-motion: reduce)').matches;
        estado.graficoVendas?.destroy();
        estado.graficoStatus?.destroy();
        Chart.defaults.global.defaultFontColor = '#495057';
        estado.graficoVendas = new Chart(document.getElementById('grafico-vendas'), {
            type: 'line',
            data: {labels: dados.vendasPorDia.map(venda => dataCurta(venda.data)), datasets: [
                {label: 'Faturamento líquido', data: dados.vendasPorDia.map(venda => Number(venda.faturamentoLiquido)), borderColor: '#007bff', backgroundColor: 'rgba(0,123,255,.08)', borderWidth: 2.5, pointRadius: dados.vendasPorDia.length <= 31 ? 2 : 0, pointHitRadius: 12, lineTension: .15, yAxisID: 'receita'},
                {label: 'Vendas válidas', data: dados.vendasPorDia.map(venda => venda.pedidos), borderColor: '#6c757d', backgroundColor: 'transparent', borderWidth: 1.5, borderDash: [4, 4], pointRadius: 0, pointHitRadius: 12, lineTension: .15, yAxisID: 'pedidos'}
            ]},
            options: {responsive: true, maintainAspectRatio: false, animation: {duration: movimentoReduzido ? 0 : 250}, legend: {position: 'bottom'},
                tooltips: {mode: 'index', intersect: false, callbacks: {label: function rotuloTooltipVenda(informacao) {
                    return informacao.datasetIndex === 0 ? ' Receita: ' + moeda.format(informacao.yLabel) : ' Vendas: ' + inteiro.format(informacao.yLabel);
                }}},
                scales: {xAxes: [{gridLines: {display: false}, ticks: {maxTicksLimit: 8, maxRotation: 0, fontSize: 10}}], yAxes: [
                    {id: 'receita', position: 'left', gridLines: {color: '#e9ecef', drawBorder: false}, ticks: {beginAtZero: true, suggestedMax: 1, maxTicksLimit: 5, fontSize: 9, callback: function rotuloEixoReceita(valor) { return moeda.format(valor); }}},
                    {id: 'pedidos', position: 'right', gridLines: {display: false}, ticks: {beginAtZero: true, suggestedMax: 1, precision: 0, maxTicksLimit: 5, fontSize: 9}, scaleLabel: {display: true, labelString: 'vendas', fontSize: 9}}
                ]}}
        });
        const situacoes = dados.situacaoPedidos.filter(situacao => situacao.quantidade > 0);
        document.getElementById('grafico-status').hidden = situacoes.length === 0;
        document.getElementById('status-sem-dados').hidden = situacoes.length !== 0;
        if (situacoes.length) {
            estado.graficoStatus = new Chart(document.getElementById('grafico-status'), {
                type: 'doughnut', data: {labels: situacoes.map(situacao => rotuloStatus(situacao.status)), datasets: [{data: situacoes.map(situacao => situacao.quantidade), backgroundColor: situacoes.map(situacao => coresStatus[situacao.status] || coresStatus.NAO_INFORMADO), borderWidth: 3, borderColor: '#fff'}]},
                options: {responsive: true, maintainAspectRatio: false, cutoutPercentage: 75, legend: {display: false}, animation: {duration: movimentoReduzido ? 0 : 250}, tooltips: {callbacks: {label: function rotuloTooltipStatus(informacao, conjunto) { return conjunto.labels[informacao.index] + ': ' + inteiro.format(conjunto.datasets[0].data[informacao.index]); }}}}
            });
        }
    }

    function aplicarPeriodo(dias) {
        if (!estado.configuracao) return;
        const fim = new Date(estado.configuracao.fimPadrao + 'T12:00:00Z');
        fimCampo.value = estado.configuracao.fimPadrao;
        fim.setUTCDate(fim.getUTCDate() - dias + 1);
        inicioCampo.value = fim.toISOString().slice(0, 10);
        formulario.requestSubmit();
    }

    async function inicializarDashboard() {
        marcarCarregamento(true);
        const controlador = new AbortController();
        const limite = setTimeout(() => controlador.abort(), 15000);
        try {
            const configuracao = await buscarJson('/api/dashboard/filtros', controlador.signal);
            estado.configuracao = configuracao;
            const parametros = new URLSearchParams(location.search);
            inicioCampo.value = parametros.get('inicio') || configuracao.inicioPadrao;
            fimCampo.value = parametros.get('fim') || configuracao.fimPadrao;
            statusCampo.replaceChildren(elemento('option', 'Todos os status'));
            statusCampo.firstElementChild.value = '';
            for (const opcao of configuracao.status) {
                const item = elemento('option', opcao.rotulo);
                item.value = opcao.valor;
                statusCampo.appendChild(item);
            }
            statusCampo.value = parametros.get('status') || '';
            await atualizarDashboard({inicio: inicioCampo.value, fim: fimCampo.value, status: parametros.get('status') || ''});
            if (!estado.intervalo) estado.intervalo = setInterval(() => {
                if (!document.hidden && !estado.carregando && document.getElementById('atualizacao-automatica').checked) atualizarDashboard();
            }, configuracao.atualizacaoSegundos * 1000);
        } catch (falha) {
            mostrarErro(falha.name === 'AbortError' ? new Error('Tempo de consulta excedido. Use o botão de atualizar para tentar novamente.') : falha);
        } finally {
            clearTimeout(limite);
            marcarCarregamento(false);
        }
    }

    formulario.addEventListener('submit', function aplicarFiltros(submissao) {
        submissao.preventDefault();
        if (!estado.configuracao) { inicializarDashboard(); return; }
        atualizarDashboard({inicio: inicioCampo.value, fim: fimCampo.value, status: statusCampo.value});
    });
    document.getElementById('atualizar-dashboard').addEventListener('click', function atualizarManualmente() {
        if (estado.configuracao && estado.filtrosAplicados) atualizarDashboard();
        else inicializarDashboard();
    });
    document.querySelectorAll('[data-periodo]').forEach(botao => botao.addEventListener('click', () => aplicarPeriodo(Number(botao.dataset.periodo))));
    window.addEventListener('pagehide', function encerrarAtualizacao() {
        estado.controlador?.abort();
        clearInterval(estado.intervalo);
    });
    inicializarDashboard();
})();
