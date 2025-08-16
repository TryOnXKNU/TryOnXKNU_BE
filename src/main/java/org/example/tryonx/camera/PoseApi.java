package org.example.tryonx.camera;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;

@RestController
@RequestMapping("/api/v1/bodytype")
@RequiredArgsConstructor
public class PoseApi {
    private final PoseStore store;

//    @GetMapping("/latest")
//    public ResponseEntity<PoseResult> latest() {
//        PoseResult r = store.getLatest();
//        return r == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(r);
//    }
//
//    @PostMapping
//    public ResponseEntity<Void> receive(@RequestBody PoseResult result) {
//        store.setLatest(result);
//        return ResponseEntity.ok().build();
//    }
}
