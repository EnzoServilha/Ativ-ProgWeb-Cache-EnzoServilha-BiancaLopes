package school.sptech.cacheconteudos.cache;
import school.sptech.cacheconteudos.entity.Conteudo;
import java.time.Instant;
public class EntradaCache {

 private final Conteudo conteudo;
 private final Instant armazenadoEm;
 private Instant ultimoAcesso;
 private long ordemAcesso;

 public EntradaCache(Conteudo conteudo, Instant agora, long ordem) {
  this.conteudo = conteudo; this.armazenadoEm = agora; acessar(agora, ordem);
 }

 public void acessar(Instant agora, long ordem) {
     ultimoAcesso = agora; ordemAcesso = ordem;
 }

 public Conteudo getConteudo() {
     return conteudo;
 }

 public Instant getArmazenadoEm() {
     return armazenadoEm;
 }


 public Instant getUltimoAcesso() {
     return ultimoAcesso;
 }

 public long getOrdemAcesso() {
     return ordemAcesso;
 }
}
