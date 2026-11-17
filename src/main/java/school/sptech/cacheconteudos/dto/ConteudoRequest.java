package school.sptech.cacheconteudos.dto;
import jakarta.validation.constraints.*;

public record ConteudoRequest(
 @NotBlank @Size(max=255)
 String titulo,
 @NotBlank @Size(max=10000)
 String corpo,
 @NotBlank @Size(max=255)
 String autor) {

}
