package ru.gang.logdoc.protocol;

import java.io.DataOutputStream;
import java.io.IOException;

/**
 * @author Denis Danilin | denis@danilin.name
 * 22.06.2021 11:00
 * logback-appender ☭ sweat and blood
 */
public enum AppProto {
    Void,
    AppendersRequestToken,
    AppendersRequestConfig,
    LogEvent,
    LogEventCompose,
    NettyTokenResponse,
    GraceStop;

    public void set(final DataOutputStream os) throws IOException {
        os.writeByte(ordinal());
    }

    public static AppProto is(byte b) {
        if (b < 0 || b >= values().length)
            return Void;

        return values()[b];
    }
}
