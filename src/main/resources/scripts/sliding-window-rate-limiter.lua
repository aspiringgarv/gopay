-- Atomic sliding-window-log rate limiter: prune + add + count + (on denial) find the oldest entry,
-- all as one indivisible step on the Redis server, closing the multi-round-trip race the plain
-- Java version has when several concurrent requests for the same key interleave their calls.
--
-- KEYS[1] = the sorted-set key for this caller
-- ARGV[1] = now, in milliseconds
-- ARGV[2] = window length, in milliseconds
-- ARGV[3] = limit
-- ARGV[4] = a unique member for this request (built in Java — Lua has no good source of real randomness)
--
-- returns {allowed (1/0), remaining, retryAfterSeconds}

local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])
local member = ARGV[4]

local floor = now - windowMs

redis.call('ZREMRANGEBYSCORE', key, 0, floor)
redis.call('ZADD', key, now, member)
redis.call('PEXPIRE', key, windowMs)

local count = redis.call('ZCARD', key)

if count <= limit then
    return {1, limit - count, 0}
end

local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
local retryAfterSeconds = math.ceil(windowMs / 1000)
if oldest[2] ~= nil then
    local oldestScore = tonumber(oldest[2])
    local msUntilFree = (oldestScore + windowMs) - now
    retryAfterSeconds = math.max(1, math.ceil(msUntilFree / 1000))
end

return {0, 0, retryAfterSeconds}
