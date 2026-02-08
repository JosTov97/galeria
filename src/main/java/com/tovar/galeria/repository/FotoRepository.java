package com.tovar.galeria.repository;

import com.tovar.galeria.model.Foto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FotoRepository extends JpaRepository<Foto, Long> {

    List<Foto> findByCategoriaOrderByFechaCapturaDesc(String categoria);
    List<Foto> findByEsFavoritaTrueOrderByFechaCapturaDesc();
    List<Foto> findAllByOrderByFechaCapturaDesc();
}
