package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ru.gang.logdoc.flaps.Sourcer;
import ru.gang.logdoc.flaps.impl.PostSourcer;
import ru.gang.logdoc.flaps.impl.PreSourcer;
import ru.gang.logdoc.flaps.impl.SimpleSourcer;
import ru.gang.logdoc.flaps.impl.SourcerBoth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static ru.gang.logdoc.utils.Tools.*;


/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:40
 * logback-adapter ☭ sweat and blood
 */
abstract class LogdocBase extends AppenderBase<ILoggingEvent> {
    protected static final String rtId = ManagementFactory.getRuntimeMXBean().getName();
    private static final String fieldsAllowed = "abcdefghijklmnopqrstuvwxyz0123456789_";

    protected final ThrowableProxyConverter tpc = new ThrowableProxyConverter();
    private final static String FieldTimeStamp = "time_src",
            FieldProcessId = "source_id",
            FieldSource = "source_name",
            FieldLevel = "level",
            FieldMessage = "log_message",
            FieldTimeRcv = "time_rcv",
            FieldIp = "source_ip";
    private static final Set<String> controlFields = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(FieldTimeStamp, FieldProcessId, FieldSource, FieldLevel, FieldMessage,
            FieldTimeRcv, FieldIp)));


    protected String host, prefix = "", suffix = "";
    protected int port;

    /* flaps */
    protected Sourcer sourcer = new SimpleSourcer();

    @Override
    public void start() {
        int errorCount = 0;

        if (port <= 0) {
            errorCount++;
            addError("No port was configured for appender " + name);
        }

        if (host == null) {
            errorCount++;
            addError("No remote host was configured for appender " + name);
        }

        if (!prefix.isEmpty() && !suffix.isEmpty())
            sourcer = new SourcerBoth(prefix, suffix);
        else if (!prefix.isEmpty())
            sourcer = new PreSourcer(prefix);
        else if (!suffix.isEmpty())
            sourcer = new PostSourcer(suffix);

        if (errorCount == 0 && subStart()) {
            tpc.start();
            super.start();
        }
    }

    protected final byte[] encode(final ILoggingEvent event) throws Exception {
        final Map<String, String> fields = new HashMap<>(0);
        StringBuilder msg = new StringBuilder(notNull(event.getFormattedMessage()));
        final int sepIdx = msg.indexOf("@@");
        String rawFields = null;

        if (sepIdx != -1) {
            rawFields = msg.substring(sepIdx + 2);

            if (rawFields.indexOf('=') != -1)
                msg.delete(sepIdx, msg.length());
            else
                rawFields = null;
        }

        if (event.getThrowableProxy() != null)
            msg.append("\n").append(tpc.convert(event));

        fields.put(FieldMessage, msg.toString());

        if (rawFields != null) {
            final StringBuilder name = new StringBuilder();
            final int len = rawFields.length();
            String pair;
            char c;

            for (int i = 1, last = 0, eq; i < len; i++)
                if (i != last && rawFields.charAt(i) == '@' && rawFields.charAt(i - 1) != '\\') {
                    pair = rawFields.substring(last, i);

                    if ((eq = pair.indexOf('=')) != -1) {
                        name.delete(0, name.length());

                        for (int j = 0; j < eq; j++)
                            if (fieldsAllowed.indexOf((c = Character.toLowerCase(pair.charAt(j)))) != -1)
                                name.append(c);

                            if (controlFields.contains(name.toString()))
                                name.append('_');

                        if (!isEmpty(name))
                            fields.put(name.toString(), pair.substring(eq + 1));
                    }
                }
        }

        fields.put(FieldTimeStamp, Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneId.systemDefault()).toLocalDateTime().format(logTimeFormat));
        fields.put(FieldProcessId, rtId);
        fields.put(FieldSource, sourcer.apply(event.getLoggerName()));
        fields.put(FieldLevel, event.getLevel() == Level.TRACE ? "LOG" : event.getLevel().levelStr);

        try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            os.write(header);
            for (final Map.Entry<String, String> e : fields.entrySet())
                writePair(e.getKey(), e.getValue(), os);

            os.write('\n');
            return os.toByteArray();
        }
    }

    private void writePair(final String key, final String value, final OutputStream daos) throws IOException {
        if (value == null)
            return;

        if (value.indexOf('\n') != -1)
            writeComplexPair(key, value, daos);
        else
            writeSimplePart(key, value, daos);
    }

    private void writeComplexPair(final String key, final String value, final OutputStream daos) throws IOException {
        daos.write(key.getBytes(StandardCharsets.UTF_8));
        daos.write('\n');
        final byte[] v = value.getBytes(StandardCharsets.UTF_8);
        writeInt(v.length, daos);
        daos.write(v);
    }

    private void writeSimplePart(final String key, final String value, final OutputStream daos) throws IOException {
        daos.write((key + "=" + value + "\n").getBytes(StandardCharsets.UTF_8));
    }

    static void writeInt(final int in, final OutputStream os) throws IOException {
        os.write((in >>> 24) & 0xff);
        os.write((in >>> 16) & 0xff);
        os.write((in >>> 8) & 0xff);
        os.write((in) & 0xff);
    }

    protected abstract boolean subStart();

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix == null ? "" : prefix.trim() + (prefix.trim().endsWith(".") ? "" : ".");
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(final String suffix) {
        this.suffix = suffix == null ? "" : (suffix.trim().startsWith(".") ? "" : ".") + suffix.trim();
    }

    public int getPort() {
        return port;
    }

    public void setPort(final int port) {
        this.port = port;
    }
}
