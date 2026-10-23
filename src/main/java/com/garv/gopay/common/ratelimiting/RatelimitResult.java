package com.garv.gopay.common.ratelimiting;

public record RatelimitResult(Boolean allowed,int remaining,int retryAfterSeconds) {
    public static RatelimitResult allowed(int remaining){
        return new RatelimitResult(true , remaining,0);
    }
    public static RatelimitResult denied(int retryAfterSeconds){
        return new RatelimitResult(false , 0,retryAfterSeconds);
    }
}
