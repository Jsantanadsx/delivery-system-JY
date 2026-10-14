# Delivery System — Projeto Diamante 02

**Java Advanced — FIAP | Delivery com microsserviços**

**Integrante:** João Victor Santana dos Santos — **RM 566063**  
**Integrante:** Yuri Fuzinatto Garzoli — **RM 561450**

Implementação do enunciado *Projeto Diamante 02 — Sistema de pedidos de delivery com microsserviços*. A API respeita os métodos, caminhos, campos e respostas consumidos pelo [app React Native fornecido pelo professor](https://github.com/joaocarloslima/diamante-delivery).

> **Estado da validação:** código-fonte, testes automatizados e scripts preparados; **compilação e execução integrada ainda precisam ser feitas com Java 25, Gradle 9 e Docker**. O ambiente usado para gerar esta entrega não dispunha de Java 25, Gradle ou acesso à distribuição Gradle. Não há alegação de aprovação dos testes sem sua execução.

## Tecnologias

- Java 25; Spring Boot **4.0.5**; Spring Cloud **2025.1.0**; Gradle **9.2.1**.
- Eureka Server: descoberta de serviços; Spring Cloud LoadBalancer: distribuição entre pagamentos.
- Spring Framework 7: `@Retryable` / `@EnableResilientMethods` para retentativas.
- Spring Data JPA / Hibernate / H2: persistência; lock pessimista e transações.
- RabbitMQ 4: `TopicExchange`, fila durável, binding e mensagens JSON.
- Spring AI **2.0.1**: `ChatClient.Builder` conectado a OpenAI ou a endpoint compatível.
- Docker Compose: RabbitMQ com console de gerenciamento.

## Arquitetura

```text
                    Eureka :8761
                         ^
                         | registra os 3 tipos de serviços
                         |
React Native        order-service :8080
  | GET /dishes ---->   |  JPA/H2: pratos e pedidos
  | POST /orders ---->  |-- PaymentClient @Retryable + @LoadBalanced
  | POST /reviews --->  |    |--> PAYMENT-SERVICE :8081 (falha ~50%)
  | POST /assistant ->  |    `--> PAYMENT-SERVICE :8082 (falha ~50%)
  |                      |--> RabbitMQ :5672
  |                      |    delivery.exchange -- reviews.new --> reviews.queue
  |                      `--> Spring AI / modelo configurado
  |                                               |
  `-- GET /reviews/ranking :8083 <------- review-service :8083
                                          buffer em memória -> H2 a cada 5s
```

**Serviços e portas**

| Serviço | Porta(s) | Papel |
|---|---|---|
| `eureka-server` | `8761` | Dashboard e registro de serviços |
| `order-service` | `8080` | Cardápio, lock de estoque, pedidos, producer RabbitMQ, Spring AI |
| `payment-service` | `8081` e `8082` | Pagamento aleatoriamente instável, múltiplas instâncias |
| `review-service` | `8083` | Listener de mensagens, agregação e ranking |
| RabbitMQ | `5672` / `15672` | Broker AMQP / painel de administração |

## Executar (Windows / IntelliJ)

### Pré-requisitos

1. **JDK 25 instalado**, `JAVA_HOME` apontando para ele (`java -version` deve mostrar 25).
2. **Docker Desktop** aberto e Engine em execução.
3. Conexão de internet **na primeira execução** para baixar Gradle e dependências do Maven Central.
4. PowerShell / terminal do IntelliJ aberto na pasta **`delivery-system`**.

Os scripts `gradlew.bat` (Windows) e `gradlew` (Linux/macOS) são *launchers de bootstrap*: baixam uma cópia local do Gradle 9.2.1 na primeira execução. Não são o wrapper oficial com `gradle-wrapper.jar`.

### 1) RabbitMQ

```powershell
docker compose up -d
```

Acesse o console em <http://localhost:15672> com usuário `guest` e senha `guest`.

### 2) Cinco terminais (ou Run Configurations do IntelliJ)

Execute **cada comando em seu próprio terminal** a partir da raiz `delivery-system`, nesta ordem:

```powershell
.\gradlew.bat :eureka-server:bootRun
```

```powershell
.\gradlew.bat :payment-service:bootRun
```

```powershell
.\gradlew.bat :payment-service:bootRun --args="--server.port=8082"
```

```powershell
.\gradlew.bat :review-service:bootRun
```

```powershell
.\gradlew.bat :order-service:bootRun
```

Na janela <http://localhost:8761>, confirme que `ORDER-SERVICE`, `REVIEW-SERVICE` e duas instâncias de `PAYMENT-SERVICE` aparecem registradas. É normal haver um pequeno atraso de registro.

> **Linux/macOS:** use `chmod +x gradlew` uma vez e execute `./gradlew :nome-do-servico:bootRun` (em vez de `.\gradlew.bat`).

### Alternativa: Gradle instalado

Se Gradle 9.2.1+ já estiver configurado no PATH, execute `gradle :eureka-server:bootRun`, etc. Os lançadores acima não são necessários.

## Assistente IA: escolha o provedor

O sistema sobe **sem chave de API** e mantém `/assistant` respondendo no contrato JSON, mas **respostas geradas por um modelo de IA dependem de configurar o provedor**. Não usamos nem incluímos chaves reais no repositório.

**OpenAI (PowerShell)**, antes de iniciar o `order-service`:

```powershell
$env:OPENAI_API_KEY = "SUA_CHAVE_FORNECIDA_PELA_OPENAI"
$env:APP_AI_ENABLED = "true"
$env:OPENAI_MODEL = "gpt-4o-mini"
.\gradlew.bat :order-service:bootRun
```

**Servidor local compatível com a API OpenAI (LM Studio ou Ollama com API compatível)**:

```powershell
$env:OPENAI_API_KEY = "local-key"
$env:OPENAI_BASE_URL = "http://localhost:1234"
$env:OPENAI_MODEL = "nome-do-modelo-disponivel"
$env:APP_AI_ENABLED = "true"
.\gradlew.bat :order-service:bootRun
```

`OPENAI_BASE_URL` aponta para a **API do modelo**, não para outro microsserviço. O modelo deve estar carregado e expondo `/v1/chat/completions`. A cada pergunta, o serviço lê pratos, preços, estoque e descrições **novamente do banco**, injeta o contexto no prompt, orienta respostas concisas em português e pede recusa educada a temas fora do restaurante.

## Endpoints exatos do contrato

| Método | URL | Corpo | Status |
|---|---|---|---|
| GET | `http://localhost:8080/dishes` | — | 200 |
| GET | `http://localhost:8080/dishes/1` | — | 200 / 404 |
| POST | `http://localhost:8080/orders` | `{"dishId":1,"quantity":1}` | 201 / 400 / 404 / 409 / 502 |
| GET | `http://localhost:8080/orders/1` | — | 200 / 404 |
| POST | `http://localhost:8080/reviews` | `{"dishId":1,"rating":5,"comment":"Great"}` | 202 sem corpo / 400 / 404 |
| POST | `http://localhost:8080/assistant` | `{"question":"Tem opção vegana?"}` | 200 com `{"answer":"..."}` |
| GET | `http://localhost:8083/reviews/ranking` | — | 200 |
| POST | `http://localhost:8081/payments` (interno) | `{"amount":39.90}` | 200 ou 500 |

**Pedido confirmado** (exemplo):

```json
{
  "id": 1,
  "dishId": 1,
  "quantity": 1,
  "totalPrice": 39.90,
  "status": "CONFIRMED",
  "createdAt": "2026-10-09T20:00:00"
}
```

Os erros conhecidos retornam `{"error":"..."}` com mensagens em inglês. Exemplos: `Dish not found`, `Dish out of stock`, `Payment service unavailable` e `Quantity must be at least 1`.

## Testar com PowerShell

```powershell
Invoke-RestMethod http://localhost:8080/dishes
Invoke-RestMethod http://localhost:8080/dishes/1
Invoke-RestMethod http://localhost:8083/reviews/ranking

Invoke-RestMethod -Uri http://localhost:8080/orders -Method Post -ContentType 'application/json' -Body '{"dishId":1,"quantity":1}'
Invoke-RestMethod -Uri http://localhost:8080/reviews -Method Post -ContentType 'application/json' -Body '{"dishId":1,"rating":5,"comment":"Great"}'
Invoke-RestMethod -Uri http://localhost:8080/assistant -Method Post -ContentType 'application/json' -Body '{"question":"Tem prato vegetariano até 40 reais?"}'
```

Depois de postar avaliações, aguarde 5 segundos e consulte novamente o ranking para verificar o flush. No RabbitMQ Management, visualize exchange `delivery.exchange`, fila `reviews.queue` e binding `reviews.new`.

## Regra principal: 50 pedidos simultâneos

O primeiro prato (`House Burger`) possui **stock inicial 10**. O método `DishRepository.findAndLockById` aplica `@Lock(PESSIMISTIC_WRITE)` e `OrderService.placeOrder` executa dentro de `@Transactional`. Somente quem obtém o lock pode ler/decrementar o estoque naquele momento. O pagamento acontece **dentro da mesma transação**; se todas as retentativas falharem, uma exceção faz rollback da redução de estoque, e a API retorna 502. Pedidos aprovados persistem com status `CONFIRMED`.

**Teste determinístico, com pagamento simulado como aprovado:**

```powershell
.\gradlew.bat :order-service:test --tests '*OrderServiceConcurrencyTest'
```

O teste verifica 50 chamadas concorrentes, **10 pedidos**, 40 conflitos `409`, estoque final `0` e rollback em falha de pagamento. O teste não depende de RabbitMQ nem de API de IA externa.

**Teste HTTP com pagamento real (50% de falha em cada tentativa):**

```powershell
python tests/concurrent_orders.py
```

Reinicie o `order-service` com banco limpo antes de testar 10 unidades (`order-service/data/`, com app desligado). Os pagamentos são randômicos; o teste integrado serve como demonstração e pode produzir alguns `502` sem representar defeito. Também espere que haja retries e que as duas instâncias de pagamento sejam usadas, visíveis nos logs.

## Backpressure / RabbitMQ

1. `order-service` valida rating `1..5` e existência do prato.
2. Publica evento **JSON** com `dishId`, `dishName`, `rating`, `comment` em `delivery.exchange`, routing key `reviews.new`.
3. Retorna **202** sem gravar avaliação no banco.
4. `review-service` consome da fila durável `reviews.queue` e agrega em `ConcurrentHashMap<Long, ReviewAggregate>`.
5. `@Scheduled(fixedDelay=5000)` transfere os acumulados para H2 em transação, restaurando o buffer se persistência falhar.
6. `GET /reviews/ranking` **lê do H2**, ordena por média decrescente e retorna `dishId`, `dishName`, `average`, `count`.

**Nota técnica:** eventos já consumidos e ainda no buffer poderão se perder se o processo cair abruptamente antes do flush. Este é um trade-off do desenho em memória explicitamente exigido no enunciado, não uma fila de persistência definitiva.

## Bônus opcional de rate limit

Para limitar `POST /orders` a **20 requisições/s** por instância (HTTP `429` com `{"error":"Too many requests"}`), configure no terminal do `order-service`:

```powershell
$env:RATE_LIMIT_ENABLED = "true"
```

O filtro usa uma janela fixa sincronizada. Por padrão é `false` para que o teste-base de **50 requisições simultâneas** permaneça isolado; ligue o recurso especificamente para demonstrar o bônus. Não há promessa de limite global em múltiplas instâncias do `order-service`.

## App React Native do professor

Baixe ou clone <https://github.com/joaocarloslima/diamante-delivery>, execute `npm install` e `npx expo start`. No app, configure o **IP da máquina** na mesma rede Wi-Fi do celular; ele utiliza a porta 8080 para cardápio/pedidos/assistente e a 8083 para o ranking. Se usar o navegador, CORS já está habilitado em ambos os serviços HTTP externos.

## Conferência pré-entrega

- [ ] JDK 25 e Docker ativos, build `./gradlew :order-service:test :review-service:test` aprovado.
- [ ] Eureka registra os três nomes e duas instâncias de pagamento.
- [ ] Logs de `PAYMENT-SERVICE` alternam entre 8081 e 8082.
- [ ] Com pagamento indisponível, `POST /orders` retorna 502 e estoque não diminui.
- [ ] Concorrência: 50 pedidos, 10 confirmações no teste determinístico, estoque 0.
- [ ] Exchange, fila durável e binding aparecem no console do RabbitMQ.
- [ ] `POST /reviews` dá 202; ranking reflete os dados após 5 segundos.
- [ ] Chave de IA configurada **somente no ambiente**, e chatbot responde com cardápio real.
- [ ] App do professor funciona sem alterações no código dele.
- [ ] Nome/RM de todos os integrantes adicionados antes do push.
- [ ] GitHub público ou acesso liberado ao professor; link enviado no **geminidev**.

## Referências técnicas

- [Spring Cloud 2025.1.0](https://spring.io/blog/2025/11/25/spring-cloud-2025-1-0-aka-oakwood-has-been-released/)
- [Spring Framework 7 — Resilience/Retry](https://docs.spring.io/spring-framework/reference/core/resilience.html)
- [Spring AI 2.x — Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html)
- [Spring Boot 4 — Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [React Native — projeto do professor](https://github.com/joaocarloslima/diamante-delivery)
