package com.example.sitp.common;

import org.springframework.stereotype.Service;

@Service
public class InMemoryVideoStorageService implements VideoStorageService {

    @Override
    public String resolvePlayableUrl(String reference) {

        return reference;
    }
}
