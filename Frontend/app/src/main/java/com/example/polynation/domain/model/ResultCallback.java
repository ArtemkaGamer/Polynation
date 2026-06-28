package com.example.polynation.domain.model;

public interface ResultCallback<T> {
    void onResult(Resource<T> result);
}
