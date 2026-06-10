package org.example.gersangtrade.watchlist.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.gersangtrade.watchlist.dto.request.WatchTargetAddRequest;
import org.example.gersangtrade.watchlist.dto.response.WatchTargetResponse;
import org.example.gersangtrade.watchlist.service.WatchlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {

    private final WatchlistService watchlistService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WatchTargetResponse>> getList(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(watchlistService.getList(userId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WatchTargetResponse> add(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WatchTargetAddRequest req) {
        WatchTargetResponse created = watchlistService.add(userId, req);
        return ResponseEntity.created(URI.create("/api/watchlist/" + created.id())).body(created);
    }

    @DeleteMapping("/{entryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> remove(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long entryId) {
        watchlistService.remove(entryId, userId);
        return ResponseEntity.noContent().build();
    }
}
