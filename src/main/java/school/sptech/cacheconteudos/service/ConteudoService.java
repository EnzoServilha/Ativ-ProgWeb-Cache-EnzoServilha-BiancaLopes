package school.sptech.cacheconteudos.service;
import school.sptech.cacheconteudos.cache.ConteudoCache;
import school.sptech.cacheconteudos.entity.Conteudo;
import school.sptech.cacheconteudos.repository.ConteudoRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.slf4j.*;
import java.time.Instant;
import java.util.function.Consumer;

@Service
public class ConteudoService {
    private static final Logger log = LoggerFactory.getLogger(ConteudoService.class);
    private final ConteudoRepository repository;
    private final ConteudoCache cache;
    private final long atrasoMs;

    public ConteudoService(ConteudoRepository repository, ConteudoCache cache,
                           @Value("${app.fonte.atraso-ms:2000}") long atrasoMs) {
        if (atrasoMs < 0) throw new IllegalArgumentException("Atraso não pode ser negativo");
        this.repository = repository;
        this.cache = cache;
        this.atrasoMs = atrasoMs;
    }

    public Conteudo buscar(Long id, Consumer<String> informarCache) {
        long inicio = System.nanoTime();
        String status = "MISS";
        try {
            synchronized (cache) {
                Conteudo conteudo = cache.buscar(id);
                if (conteudo != null) status = "HIT";
                else {
                    aguardarFonte();
                    conteudo = encontrar(id);
                    cache.armazenar(conteudo);
                }
                informarCache.accept(status);
                return conteudo;
            }
        } finally {
            log.info("Cache {} - id={} - {} ms", status, id, (System.nanoTime() - inicio) / 1_000_000);
        }
    }

    public Conteudo cadastrar(Conteudo conteudo) {
        conteudo.setId(null);
        conteudo.setAtualizadoEm(Instant.now());
        return repository.saveAndFlush(conteudo);
    }

    public Conteudo atualizar(Long id, Conteudo novo) {
        synchronized (cache) {
            Conteudo atual = encontrar(id);
            atual.setTitulo(novo.getTitulo());
            atual.setCorpo(novo.getCorpo());
            atual.setAutor(novo.getAutor());
            atual.setAtualizadoEm(Instant.now());
            Conteudo salvo = repository.saveAndFlush(atual);
            cache.invalidar(id);
            return salvo;
        }
    }

    public void excluir(Long id) {
        synchronized (cache) {
            repository.delete(encontrar(id));
            cache.invalidar(id);
        }
    }

    private Conteudo encontrar(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conteúdo não encontrado"));
    }

    private void aguardarFonte() {
        try {
            Thread.sleep(atrasoMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Busca interrompida", e);
        }
    }
}
