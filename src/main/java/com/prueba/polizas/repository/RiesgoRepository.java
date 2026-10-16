package com.prueba.polizas.repository;

import com.prueba.polizas.model.Riesgo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiesgoRepository extends JpaRepository<Riesgo, Long> {

    List<Riesgo> findByPolizaIdOrderByIdAsc(Long polizaId);
}
