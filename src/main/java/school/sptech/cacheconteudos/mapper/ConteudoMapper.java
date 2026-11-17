package school.sptech.cacheconteudos.mapper;
import school.sptech.cacheconteudos.dto.*;
import school.sptech.cacheconteudos.dto.ConteudoRequest;
import school.sptech.cacheconteudos.dto.ConteudoResponse;
import school.sptech.cacheconteudos.entity.Conteudo;
public class ConteudoMapper {
    private ConteudoMapper() {
    }

    public static Conteudo toEntity(ConteudoRequest dto) {
        Conteudo c = new Conteudo();
        c.setTitulo(dto.titulo());
        c.setCorpo(dto.corpo());
        c.setAutor(dto.autor());
        return c;
    }

    public static ConteudoResponse toDto(Conteudo c) {
        return new ConteudoResponse(c.getId(), c.getTitulo(), c.getCorpo(), c.getAutor(), c.getAtualizadoEm());
    }
}
