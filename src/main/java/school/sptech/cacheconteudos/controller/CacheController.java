package school.sptech.cacheconteudos.controller;
import school.sptech.cacheconteudos.cache.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import school.sptech.cacheconteudos.cache.ConteudoCache;
import school.sptech.cacheconteudos.cache.EstatisticasCache;

@RestController
@RequestMapping("/cache")
public class CacheController {
 private final ConteudoCache cache;
 public CacheController(ConteudoCache cache) {
     this.cache = cache;
 }

 @GetMapping("/estatisticas")
 public EstatisticasCache estatisticas() {
     return cache.estatisticas();
 }

 @DeleteMapping
 public ResponseEntity<Void> limpar() {
     cache.limpar(); return ResponseEntity.noContent().build();
 }
}
