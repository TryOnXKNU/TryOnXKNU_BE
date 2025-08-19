package org.example.tryonx.returns.controller;

import lombok.RequiredArgsConstructor;
import org.example.tryonx.enums.ReturnStatus;
import org.example.tryonx.returns.dto.ReturnDetailDto;
import org.example.tryonx.returns.dto.ReturnListDto;
import org.example.tryonx.returns.service.ReturnService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/admin/returns")
public class AdminReturnsController {
    private final ReturnService returnService;

    //전체 반품 목록 조회
    @GetMapping("/all")
    public ResponseEntity<List<ReturnListDto>> getAllReturns() {
        List<ReturnListDto> returnList = returnService.getReturnList();
        return ResponseEntity.ok(returnList);
    }

    //반품 상세 정보
    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnDetailDto> getReturnDetail(@PathVariable Integer returnId) {
        ReturnDetailDto dto = returnService.findByReturnIdForAdmin(returnId);
        return ResponseEntity.ok(dto);
    }

    //반품 상태 변경
    @PatchMapping("/{returnId}/status/{status}")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Integer returnId,
            @PathVariable ReturnStatus status,
            @RequestParam(required = false) String reason
    ) {
        returnService.updateReturnStatus(returnId, status, reason);
        return ResponseEntity.ok().build();
    }

    //반품 상태별 조회
//    @GetMapping("/status")
//    public ResponseEntity<List<ReturnListDto>> getReturnsByStatus(
//            @RequestParam("status") ReturnStatus status
//    ) {
//        List<ReturnListDto> result = returnService.getReturnsByStatus(status);
//        return ResponseEntity.ok(result);
//    }
}
