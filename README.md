# Cache de conteúdos — Spring Boot

Bianca Ortega & Enzo Servilha

## Tecnologias e execução

Java 21+, Spring Boot 4.0.0, Maven, Spring Web MVC, JPA e H2.
O H2 fica em memória: ao reiniciar, conteúdos e estatísticas são perdidos.

1. Clone o repositorio em sua máquina
2. Selecione JDK 21 no projeto e aguarde a importação Maven.
3. Execute `CacheConteudosApplication`.
4. A API esta em `http://localhost:8080`.

Pelo terminal, com Maven instalado:

```bash
mvn test
mvn spring-boot:run
```

## Organização

| Pacote | Responsabilidade |
| --- | --- |
| entity | Conteudo persistido no banco |
| repository | Acesso JPA ao H2 |
| dto | Entrada validada e saída HTTP |
| mapper | Conversão entre DTO e entidade, chamada somente na controller |
| controller | Rotas, status HTTP, DTOs e header X-Cache |
| service | Regras, atraso da fonte, banco e uso do cache |
| cache | ConcurrentHashMap, entrada, LRU, TTL e estatísticas |

## Como o cache funciona

1. O service consulta `ConteudoCache.buscar(id)`.
2. Se a entrada existe e não expirou: registra HIT e atualiza último acesso.
3. Caso contrário: registra MISS, espera 2 segundos, consulta o repositório e armazena o resultado.
4. Antes de inserir, descarta expirados. Se ainda estiver cheio, remove o item com menor último acesso (LRU).
5. Um contador de ordem desempata acessos com o mesmo horário.
6. PUT e DELETE invalidam a entrada após a operação de banco concluir.

O TTL começa quando o item entra no cache; um HIT **não renova** esse prazo. Ao atingir o TTL, a entrada expira. Não há rotina agendada: expirados são descartados na consulta do item, em inserções e na leitura das estatísticas.

TTL e limite devem ser positivos; atraso pode ser zero para testes.

O mapa é `Map<Long, EntradaCache>` implementado por `ConcurrentHashMap`. Métodos do cache são `synchronized`, protegendo também contadores e a sequência verificar/remover/inserir. O service usa o mesmo monitor nas buscas, atualizações e exclusões, evitando que uma busca anterior recoloque dados antigos depois de uma invalidação.

As entidades do cache são apenas lidas pelo GET; não devem ser modificadas diretamente por outros componentes. A aplicação usa `open-in-view=false` e não mantém uma transação externa envolvendo cache e repositório. Os métodos de escrita do repositório concluem suas transações antes da invalidação.

### Estatísticas

- `tamanhoAtual`: apenas entradas válidas.
- `limite`: máximo configurado.
- `hits`: consultas encontradas e válidas.
- `misses`: consultas sem entrada válida, inclusive buscas de IDs inexistentes.
- `evictions`: remoções por capacidade; TTL, PUT, DELETE e limpeza não incrementam.
- `hitRatio`: `hits / (hits + misses)`, de 0 a 1; multiplique por 100 para obter porcentagem. Sem consultas, retorna 0.

`DELETE /cache` limpa entradas e **preserva contadores**. Reinicie para zerá-los (o H2 também será reiniciado). POST não preenche cache. Conteúdos inexistentes não são armazenados.

O log do service mede a busca inteira, incluindo espera e banco no MISS, com SLF4J. Não é exatamente o tempo do Postman, que também inclui rede e serialização.

## Rotas

| Método | Rota | Resultado |
| --- | --- | --- |
| POST | /conteudos | 201, conteúdo criado e Location |
| GET | /conteudos/{id} | 200 com X-Cache: HIT ou MISS; 404 se inexistente |
| PUT | /conteudos/{id} | 200 e invalidação; 404 se inexistente |
| DELETE | /conteudos/{id} | 204 e invalidação; 404 se inexistente |
| GET | /cache/estatisticas | 200 e contadores |
| DELETE | /cache | 204, entradas removidas |

POST e PUT recebem JSON com os três campos obrigatórios (texto não vazio):

```json
{
  "titulo": "Introdução ao Spring Boot",
  "corpo": "Este é o texto do meu primeiro conteúdo.",
  "autor": "Bianca"
}
```

`id` e `atualizadoEm` são definidos pela aplicação. JSON inválido ou campos vazios retornam 400. Título/autor aceitam até 255 caracteres e corpo até 10000.

## Testes automatizados

`mvn test` executa testes de LRU, desempate, expiração sem renovar TTL no HIT, descarte de expirados sem eviction, invalidação, limpeza, razão de acertos, configuração e inserções concorrentes. O roteiro HTTP valida também integração com banco, headers e endpoints.
# Ativ-ProgWeb-Cache-EnzoServilha-BiancaLopes
