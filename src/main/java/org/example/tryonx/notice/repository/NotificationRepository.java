package org.example.tryonx.notice.repository;

import org.example.tryonx.member.domain.Member;
import org.example.tryonx.notice.domain.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByMember(Member member);
}
