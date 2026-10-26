package com.garv.gopay.common.idempotency;

import com.garv.gopay.merchant.context.MerchantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

// not a @Component on purpose: it is created in WebSecurityConfig and added AFTER the JWT/API-key
// authentication filters in each chain, so MerchantContext is already populated by the time this runs —
// the idempotency key is scoped per merchant, so two merchants can safely reuse the same literal key
@Slf4j
@RequiredArgsConstructor
public class IdempotencyFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Idempotency-Key";
    // how long the "IN_PROGRESS" claim lives — just needs to outlast one request's processing time,
    // as a safety net in case delete() never runs (a crash mid-request)
    private static final Duration CLAIM_TTL = Duration.ofSeconds(30);
    // how long the real stored result lives for replay — long enough for a legitimately slow client
    // retry to still get the same response back, not just the one request's own lifetime
    private static final Duration RESULT_TTL = Duration.ofHours(24);
    // GET is already safe to repeat and DELETE is naturally idempotent on its own; only the methods
    // that can create or mutate state on every call need a guard against being run twice
    private static final Set<String> GUARDED_METHODS = Set.of("POST", "PUT", "PATCH");

    private final IdempotencyStore idempotencyStore;
    private final MerchantContext merchantContext;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!GUARDED_METHODS.contains(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawKey = request.getHeader(HEADER);
        if (rawKey == null || rawKey.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        // safe to call here — this filter only runs after a JWT/API-key filter has already called
        // filterChain.doFilter(), which only happens once authentication actually succeeded
        String key = merchantContext.requireMerchantId() + ":" + rawKey;

        boolean claimed = idempotencyStore.setIfAbsent(key, CLAIM_TTL);
        if (!claimed) {
            replayOrReject(key, response);
            return;
        }

        // wrap the response so the body can be read back after the real controller has written it —
        // a servlet response is normally write-only, this is what makes replaying it later possible
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrappedResponse);

            if (wrappedResponse.getStatus() < 400) {
                String body = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
                idempotencyStore.store(key, body, RESULT_TTL);
            } else {
                // this attempt did not actually succeed — release the key so a real retry gets through
                idempotencyStore.delete(key);
            }
        } catch (Exception ex) {
            idempotencyStore.delete(key);
            throw ex;
        } finally {
            // the wrapper only buffered the body — this is what actually sends it to the real client
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void replayOrReject(String key, HttpServletResponse response) throws IOException {
        Optional<String> stored = idempotencyStore.get(key);

        if (stored.isPresent() && !IdempotencyStore.IN_PROGRESS.equals(stored.get())) {
            log.debug("Replaying stored result for idempotency key {}", key);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write(stored.get());
            return;
        }

        log.warn("Rejecting duplicate request for idempotency key {}", key);
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"errorCode\":\"DUPLICATE_IDEMPOTENCY_KEY\",\"errorDescription\":\"A request with this idempotency key is already in progress or was already processed.\"}");
    }

}
