package com.utp.gajog.support;

import java.util.*;
import java.util.function.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.FluentQuery;

/** Almacén Java exclusivo de pruebas. Las operaciones JPA ajenas al escenario fallan explícitamente. */
public abstract class RepositorioEnMemoria<T> implements JpaRepository<T, Long> {
    public final Map<Long, T> datos = new LinkedHashMap<>();
    public final List<T> guardados = new ArrayList<>();
    private final Function<T, Long> obtenerId;
    private final BiConsumer<T, Long> asignarId;

    protected RepositorioEnMemoria(Function<T, Long> obtenerId, BiConsumer<T, Long> asignarId) {
        this.obtenerId = obtenerId;
        this.asignarId = asignarId;
    }

    public void agregar(T entidad) {
        Long id = obtenerId.apply(entidad);
        if (id == null) {
            id = datos.keySet().stream().max(Long::compareTo).orElse(0L) + 1;
            asignarId.accept(entidad, id);
        }
        datos.put(id, entidad);
    }

    @Override
    public <S extends T> S save(S entidad) {
        agregar(entidad);
        guardados.add(entidad);
        return entidad;
    }

    @Override public Optional<T> findById(Long id) { return Optional.ofNullable(datos.get(id)); }
    @Override public List<T> findAll() { return new ArrayList<>(datos.values()); }
    @Override public boolean existsById(Long id) { return datos.containsKey(id); }
    @Override public long count() { return datos.size(); }

    private UnsupportedOperationException noImplementado() {
        return new UnsupportedOperationException("Operación JPA fuera del alcance del almacén de pruebas");
    }

    @Override public <S extends T> List<S> saveAll(Iterable<S> entidades) { throw noImplementado(); }
    @Override public List<T> findAllById(Iterable<Long> ids) { throw noImplementado(); }
    @Override public void deleteById(Long id) { throw noImplementado(); }
    @Override public void delete(T entidad) { throw noImplementado(); }
    @Override public void deleteAllById(Iterable<? extends Long> ids) { throw noImplementado(); }
    @Override public void deleteAll(Iterable<? extends T> entidades) { throw noImplementado(); }
    @Override public void deleteAll() { throw noImplementado(); }
    @Override public List<T> findAll(Sort orden) { throw noImplementado(); }
    @Override public Page<T> findAll(Pageable pagina) { throw noImplementado(); }
    @Override public void flush() { throw noImplementado(); }
    @Override public <S extends T> S saveAndFlush(S entidad) { throw noImplementado(); }
    @Override public <S extends T> List<S> saveAllAndFlush(Iterable<S> entidades) { throw noImplementado(); }
    @Override public void deleteAllInBatch(Iterable<T> entidades) { throw noImplementado(); }
    @Override public void deleteAllByIdInBatch(Iterable<Long> ids) { throw noImplementado(); }
    @Override public void deleteAllInBatch() { throw noImplementado(); }
    @Override public T getOne(Long id) { throw noImplementado(); }
    @Override public T getById(Long id) { throw noImplementado(); }
    @Override public T getReferenceById(Long id) { throw noImplementado(); }
    @Override public <S extends T> Optional<S> findOne(Example<S> ejemplo) { throw noImplementado(); }
    @Override public <S extends T> List<S> findAll(Example<S> ejemplo) { throw noImplementado(); }
    @Override public <S extends T> List<S> findAll(Example<S> ejemplo, Sort orden) { throw noImplementado(); }
    @Override public <S extends T> Page<S> findAll(Example<S> ejemplo, Pageable pagina) { throw noImplementado(); }
    @Override public <S extends T> long count(Example<S> ejemplo) { throw noImplementado(); }
    @Override public <S extends T> boolean exists(Example<S> ejemplo) { throw noImplementado(); }
    @Override public <S extends T, R> R findBy(Example<S> ejemplo,
            Function<FluentQuery.FetchableFluentQuery<S>, R> consulta) { throw noImplementado(); }
}
