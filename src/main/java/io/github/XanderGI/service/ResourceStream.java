package io.github.XanderGI.service;

import java.io.IOException;
import java.io.OutputStream;

@FunctionalInterface
public interface ResourceStream {
    void writeTo(OutputStream outputStream) throws IOException;
}