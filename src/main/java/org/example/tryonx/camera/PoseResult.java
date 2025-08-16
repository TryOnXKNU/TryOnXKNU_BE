package org.example.tryonx.camera;

import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class PoseResult {
    private long frameTs;

    // 추가
    private Long memberId;

    private String bodyType;       // STRAIGHT | WAVE | NATURAL
    private List<Landmark> landmarks;
    @Data
    public static class Landmark {
        private double x; private double y; private double z; private Double visibility;
    }
    public Instant serverReceivedAt = Instant.now();
}
