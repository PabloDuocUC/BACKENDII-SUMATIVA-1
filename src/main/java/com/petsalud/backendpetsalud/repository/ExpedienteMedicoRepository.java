package com.petsalud.backendpetsalud.repository;

import com.petsalud.backendpetsalud.entity.ExpedienteMedico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpedienteMedicoRepository extends JpaRepository<ExpedienteMedico, Long> {
    List<ExpedienteMedico> findByMascotaId(Long mascotaId);
}
