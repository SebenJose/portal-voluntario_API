# Demonstração removida

A aplicação utiliza exclusivamente a API Java. Os handlers MSW, fixtures,
IndexedDB de certificados, catálogo estático e rotas de demonstração foram removidos.

Um componente de transição remove apenas o antigo `mockServiceWorker.js`, caso
este navegador tenha usado a versão anterior. Ele não registra nenhum worker novo.

Veja [configuração e execução](../README.md).
