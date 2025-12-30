package io.github.gohoski.numai;

interface ApiCallback<T> {
    void onSuccess(T result);
    void onError(ApiError error);
}