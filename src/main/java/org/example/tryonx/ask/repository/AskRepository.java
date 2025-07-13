package org.example.tryonx.ask.repository;

import org.example.tryonx.ask.domain.Ask;
import org.example.tryonx.enums.AnswerStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AskRepository extends JpaRepository<Ask, Long> {
    List<Ask> findByMemberEmail(String email);
    List<Ask> findByAnswerStatus(AnswerStatus status);

}
