package school.sptech.cacheconteudos.cache;
import school.sptech.cacheconteudos.entity.Conteudo;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.*;
import org.slf4j.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConteudoCache {

 private static final Logger log = LoggerFactory.getLogger(ConteudoCache.class);
 private final Map<Long, EntradaCache> entradas = new ConcurrentHashMap<>();
 private final int limite;
 private final Duration ttl;
 private final Clock clock;
 private long hits, misses, evictions, sequencia;

 @Autowired
 public ConteudoCache(@Value("${app.cache.max-itens:5}") int limite, @Value("${app.cache.ttl-segundos:60}") long ttlSegundos) {
  this(limite, ttlSegundos, Clock.systemUTC());
 }

 public ConteudoCache(int limite, long ttlSegundos, Clock clock) {
  if (limite < 1 || ttlSegundos < 1) throw new IllegalArgumentException("Limite e TTL devem ser positivos");
  this.limite = limite; this.ttl = Duration.ofSeconds(ttlSegundos); this.clock = clock;
 }

 public synchronized Conteudo buscar(Long id) {
  EntradaCache entrada = entradas.get(id);
  Instant agora = clock.instant();

  if (entrada == null || expirou(entrada, agora)) {
   if (entrada != null) { entradas.remove(id); log.info("Cache TTL expirado - id={}", id); }
   misses++; return null;
  }

  entrada.acessar(agora, ++sequencia);
  hits++;
  return entrada.getConteudo();
 }

 public synchronized void armazenar(Conteudo conteudo) {

  removerExpirados();

  if (!entradas.containsKey(conteudo.getId()) && entradas.size() >= limite) {
   Long idLru = entradas.entrySet().stream()
    .min(Comparator.<Map.Entry<Long, EntradaCache>, Instant>comparing(e -> e.getValue().getUltimoAcesso())
     .thenComparingLong(e -> e.getValue().getOrdemAcesso()))
    .orElseThrow().getKey();
   entradas.remove(idLru); evictions++;
   log.info("Cache EVICTION LRU - id={}", idLru);
  }

  entradas.put(conteudo.getId(), new EntradaCache(conteudo, clock.instant(), ++sequencia));
 }

 private boolean expirou(EntradaCache entrada, Instant agora) {
  return !agora.isBefore(entrada.getArmazenadoEm().plus(ttl));
 }

 private void removerExpirados() {

  Instant agora = clock.instant();
  entradas.entrySet().removeIf(e -> expirou(e.getValue(), agora));
 }

 public synchronized void invalidar(Long id) {
  entradas.remove(id); log.info("Cache INVALIDADO - id={}", id);
 }

 public synchronized void limpar() {

  entradas.clear(); log.info("Cache LIMPO (contadores preservados)");
 }

 public synchronized EstatisticasCache estatisticas() {

  removerExpirados();

  long total = hits + misses;

  return new EstatisticasCache(entradas.size(), limite, hits, misses, evictions,
    total == 0 ? 0.0 : (double) hits / total);
 }
}
