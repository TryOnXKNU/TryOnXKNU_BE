package org.example.tryonx.notice.service;

import org.example.tryonx.member.domain.Member;
import org.example.tryonx.member.repository.MemberRepository;
import org.example.tryonx.notice.domain.Notification;
import org.example.tryonx.notice.dto.NoticeResponseDto;
import org.example.tryonx.notice.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NoticeService {
    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    public NoticeService(NotificationRepository notificationRepository, MemberRepository memberRepository) {
        this.notificationRepository = notificationRepository;
        this.memberRepository = memberRepository;
    }

    public List<NoticeResponseDto> findByEmail(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("해당 이메일의 회원이 없습니다."));

        List<Notification> notifications = notificationRepository.findByMember(member);

        return notifications.stream()
                .map(n -> new NoticeResponseDto(
                        n.getNotificationId(),
                        n.getTitle(),
                        n.getContent(),
                        n.getCreatedAt()
                )).toList();
    }

}
