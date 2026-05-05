package com.combostrap.s3;

import com.combostrap.type.MediaType;

public class AwsObject {
    public AwsObject(String path) {

    }

    public static AwsObject create(String path) {
        return new AwsObject(path);
    }

    public AwsObject setContent(byte[] bytes) {
        return this;
    }

    public AwsObject setMediaType(MediaType mediaType) {
        return this;
    }
}
