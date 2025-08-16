package org.example.tryonx.camera;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class PoseStore {
    private final AtomicReference<PoseResult> latest = new AtomicReference<>();
    public void setLatest(PoseResult r) { latest.set(r); }
    public PoseResult getLatest() { return latest.get(); }
}
