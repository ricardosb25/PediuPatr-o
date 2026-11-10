async function executarPedido(caminho, configuracao) {
    const feedback = document.getElementById('feedback-pedido');
    try {
        const resposta = await fetch(caminho, configuracao);
        if (!resposta.ok) {
            const corpo = await resposta.json().catch(() => ({}));
            throw new Error(corpo.erro || 'Operação não permitida. Confira os dados e seu acesso.');
        }
        location.reload();
    } catch (falha) { feedback.textContent = falha.message; feedback.hidden = false; }
}
function adicionarItemPedido() {
    const listaItens = document.getElementById('itens-pedido');
    if (listaItens.children.length >= 100) return;
    const novoItem = listaItens.firstElementChild.cloneNode(true);
    novoItem.querySelector('select').value = '';
    novoItem.querySelector('input').value = '1';
    listaItens.appendChild(novoItem);
}
document.getElementById('novo-pedido')?.addEventListener('submit', function registrarPedido(submissao) {
    submissao.preventDefault();
    const itens = Array.from(document.querySelectorAll('.item-pedido')).map(linha => ({
        produtoId: linha.querySelector('select').value, quantidade: Number(linha.querySelector('input').value)
    }));
    executarPedido('/api/pedidos', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({clienteId:document.getElementById('cliente-pedido').value, itens})});
});
function alterarStatusPedido(botao, status) {
    executarPedido('/api/pedidos/' + encodeURIComponent(botao.parentElement.dataset.pedido) + '/status?status=' + encodeURIComponent(status), {method:'PUT'});
}
function descontarPedido(botao) {
    const percentual = prompt('Desconto percentual (0 a 20). Substitui o percentual anterior.');
    if (percentual === null || percentual.trim() === '') return;
    executarPedido('/api/pedidos/' + encodeURIComponent(botao.parentElement.dataset.pedido) + '/desconto', {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({percentual:Number(percentual.replace(',', '.'))})});
}
function cancelarPedido(botao) {
    const justificativa = prompt('Justificativa obrigatória do cancelamento:');
    if (justificativa === null) return;
    executarPedido('/api/pedidos/' + encodeURIComponent(botao.parentElement.dataset.pedido) + '/cancelamento', {method:'PUT', headers:{'Content-Type':'application/json'}, body:JSON.stringify({justificativa})});
}
