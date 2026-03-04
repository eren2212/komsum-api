package com.ereniridere.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ereniridere.entity.Neighborhood;

@Repository
public interface NeighborhoodRepository extends JpaRepository<Neighborhood, Integer> {

}
