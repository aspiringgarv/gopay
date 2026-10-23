-- Atomic token bucket: a bucket holds up to `capacity` tokens, refilling continuously at
-- capacity/windowMs tokens per millisecond. Every request costs one token; no token, no request.
-- Unlike the fixed/sliding window counters, a token bucket lets a caller burst up to the full
-- capacity at once after sitting idle, then throttles back down to the steady average rate —
-- bursty-but-bounded, instead of a hard wall per discrete window.
--
-- KEYS[1] = the bucket's hash key
-- ARGV[1] = now, in milliseconds
-- ARGV[2] = window length, in milliseconds (time to refill the bucket from empty to full)
-- ARGV[3] = capacity (max tokens the bucket can hold)
--
-- returns {allowed (1/0), remaining, retryAfterSeconds}

local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local capacity = tonumber(ARGV[3])

local refillRatePerMs = capacity / windowMs

local bucket = redis.call('HMGET', key, 'tokens', 'timestamp')
local tokens = tonumber(bucket[1])
local lastRefill = tonumber(bucket[2])

-- first time this key is ever seen: start full, as if it had been idle forever
if tokens == nil then
    tokens = capacity
    lastRefill = now
end

local elapsedMs = now - lastRefill
if elapsedMs < 0 then
    elapsedMs = 0
end

tokens = math.min(capacity, tokens + elapsedMs * refillRatePerMs)

local allowed = 0
local retryAfterSeconds = 0

if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
else
    local deficit = 1 - tokens
    local msUntilNextToken = deficit / refillRatePerMs
    retryAfterSeconds = math.max(1, math.ceil(msUntilNextToken / 1000))
end

redis.call('HSET', key, 'tokens', tokens, 'timestamp', now)
-- generous idle cleanup — well past one full refill cycle, so a quiet bucket doesn't linger forever
redis.call('PEXPIRE', key, windowMs * 2)

return {allowed, math.floor(tokens), retryAfterSeconds}
