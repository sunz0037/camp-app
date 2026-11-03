package com.camp.app.repository;

import com.camp.app.entity.OaVisitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OaVisitorRepository extends JpaRepository<OaVisitor, Long> {
    Optional<OaVisitor> findByVisitNo(String visitNo);
}
