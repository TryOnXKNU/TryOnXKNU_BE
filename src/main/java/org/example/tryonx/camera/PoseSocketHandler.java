package org.example.tryonx.camera;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.tryonx.fitting.service.FittingService;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class PoseSocketHandler implements WebSocketHandler {
    private final ObjectMapper mapper = new ObjectMapper();
    private final PoseStore store;
    private final FittingService fittingService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("WS connected: {}", session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        PoseResult result = mapper.readValue(payload, PoseResult.class);

        // 최신 PoseResult 메모리 저장
        store.setLatest(result);

        // ★ 회원 BodyShape 업데이트 호출
        try {
            fittingService.updateMemberBodyShape(result.getMemberId(), result.getBodyType());
        } catch (Exception e) {
            log.warn("updateMemberBodyShape failed. memberId={}, bodyType={}, err={}",
                    result.getMemberId(), result.getBodyType(), e.toString());
        }
        log.info("recv frameTs={}, bodyType={}, points={}",
                result.getFrameTs(),
                result.getBodyType(),
                result.getLandmarks() == null ? 0 : result.getLandmarks().size());
        session.sendMessage(new TextMessage("{\"ok\":true}"));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("WS error", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        log.info("WS closed: {}", session.getId());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    };
    }

