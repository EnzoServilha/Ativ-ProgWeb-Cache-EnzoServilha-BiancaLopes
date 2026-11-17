package school.sptech.cacheconteudos.cache;
import school.sptech.cacheconteudos.entity.Conteudo;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ConteudoCacheTest {
 private static class RelogioTeste extends Clock {
  Instant agora = Instant.EPOCH;
  public ZoneId getZone() { return ZoneOffset.UTC; }
  public Clock withZone(ZoneId zone) { return this; }
  public Instant instant() { return agora; }
 }
 private Conteudo conteudo(long id) { Conteudo c = new Conteudo(); c.setId(id); return c; }
 @Test void lruRemoveMenosRecenteMesmoComHorariosIguais() {
  var cache = new ConteudoCache(2, 60, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
  cache.armazenar(conteudo(1)); cache.armazenar(conteudo(2));
  assertNotNull(cache.buscar(1L));
  cache.armazenar(conteudo(3));
  assertNull(cache.buscar(2L));
  assertNotNull(cache.buscar(1L)); assertNotNull(cache.buscar(3L));
  assertEquals(1, cache.estatisticas().evictions());
  assertEquals(2, cache.estatisticas().tamanhoAtual());
 }
 @Test void hitNaoRenovaTtlEExpiraNoLimite() {
  RelogioTeste clock = new RelogioTeste();
  clock.agora = Instant.EPOCH;
  var cache = new ConteudoCache(2, 60, clock);
  cache.armazenar(conteudo(1));
  clock.agora = Instant.EPOCH.plusSeconds(59);
  assertNotNull(cache.buscar(1L));
  clock.agora = Instant.EPOCH.plusSeconds(60);
  assertNull(cache.buscar(1L));
  assertEquals(0, cache.estatisticas().evictions());
 }
 @Test void expiradosNaoContamComoEviction() {
  RelogioTeste clock = new RelogioTeste();
  clock.agora = Instant.EPOCH;
  var cache = new ConteudoCache(1, 60, clock);
  cache.armazenar(conteudo(1));
  clock.agora = Instant.EPOCH.plusSeconds(61);
  cache.armazenar(conteudo(2));
  assertEquals(0, cache.estatisticas().evictions());
  assertEquals(1, cache.estatisticas().tamanhoAtual());
 }
 @Test void invalidacaoLimpezaEEstatisticas() {
  var cache = new ConteudoCache(5, 60);
  assertEquals(0, cache.estatisticas().hitRatio());
  assertNull(cache.buscar(1L));
  cache.armazenar(conteudo(1)); assertNotNull(cache.buscar(1L));
  assertEquals(0.5, cache.estatisticas().hitRatio());
  cache.invalidar(1L); assertNull(cache.buscar(1L));
  cache.armazenar(conteudo(2)); cache.limpar();
  assertEquals(0, cache.estatisticas().tamanhoAtual());
  assertEquals(1, cache.estatisticas().hits()); assertEquals(2, cache.estatisticas().misses());
 }
 @Test void concorrenciaRespeitaLimite() throws Exception {
  var cache = new ConteudoCache(5, 60);
  try (var executor = Executors.newFixedThreadPool(10)) {
   var tarefas = new java.util.ArrayList<Future<?>>();
   for (long i = 1; i <= 100; i++) { long id = i; tarefas.add(executor.submit(() -> cache.armazenar(conteudo(id)))); }
   for (Future<?> tarefa : tarefas) tarefa.get();
  }
  assertEquals(5, cache.estatisticas().tamanhoAtual());
  assertEquals(95, cache.estatisticas().evictions());
 }
 @Test void rejeitaConfiguracaoInvalida() {
  assertThrows(IllegalArgumentException.class, () -> new ConteudoCache(0, 60));
  assertThrows(IllegalArgumentException.class, () -> new ConteudoCache(5, 0));
 }
}
