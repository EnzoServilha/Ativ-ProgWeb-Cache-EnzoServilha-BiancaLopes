package school.sptech.cacheconteudos.controller;
import school.sptech.cacheconteudos.dto.*;
import school.sptech.cacheconteudos.dto.ConteudoRequest;
import school.sptech.cacheconteudos.dto.ConteudoResponse;
import school.sptech.cacheconteudos.mapper.ConteudoMapper;
import school.sptech.cacheconteudos.service.ConteudoService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import jakarta.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/conteudos")
public class ConteudoController {

 private final ConteudoService service;

 public ConteudoController(ConteudoService service) {
     this.service = service;
 }

 @GetMapping("/{id}")
 public ResponseEntity<ConteudoResponse> buscar(@PathVariable Long id) {

  HttpHeaders headers = new HttpHeaders();
  var conteudo = service.buscar(id, status -> headers.set("X-Cache", status));
  return ResponseEntity.ok().headers(headers).body(ConteudoMapper.toDto(conteudo));
 }

 @PostMapping
 public ResponseEntity<ConteudoResponse> cadastrar(@Valid @RequestBody ConteudoRequest dto) {

  var salvo = service.cadastrar(ConteudoMapper.toEntity(dto));
  return ResponseEntity.created(URI.create("/conteudos/" + salvo.getId())).body(ConteudoMapper.toDto(salvo));
 }

 @PutMapping("/{id}")
 public ResponseEntity<ConteudoResponse> atualizar(@PathVariable Long id, @Valid @RequestBody ConteudoRequest dto) {
  return ResponseEntity.ok(ConteudoMapper.toDto(service.atualizar(id, ConteudoMapper.toEntity(dto))));
 }

 @DeleteMapping("/{id}")
 public ResponseEntity<Void> excluir(@PathVariable Long id) {
  service.excluir(id); return ResponseEntity.noContent().build();
 }
}
