package school.sptech.cacheconteudos.cache;
public record EstatisticasCache(int tamanhoAtual, int limite, long hits, long misses, long evictions, double hitRatio) {
}
