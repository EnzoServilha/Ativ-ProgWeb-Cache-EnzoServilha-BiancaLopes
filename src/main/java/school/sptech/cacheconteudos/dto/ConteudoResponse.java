package school.sptech.cacheconteudos.dto;
import java.time.Instant;
public record ConteudoResponse(Long id, String titulo, String corpo, String autor, Instant atualizadoEm) {
}
