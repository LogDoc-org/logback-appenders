package org.logdoc.appenders;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import org.logdoc.flaps.Sourcer;
import org.logdoc.flaps.impl.PostSourcer;
import org.logdoc.flaps.impl.PreSourcer;
import org.logdoc.flaps.impl.SimpleSourcer;
import org.logdoc.flaps.impl.SourcerBoth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

import static org.logdoc.LogDocConstants.Fields.*;
import static org.logdoc.LogDocConstants.header;
import static org.logdoc.LogDocConstants.logTimeFormat;
import static org.logdoc.helpers.Texts.isEmpty;
import static org.logdoc.helpers.Texts.notNull;


/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:40
 * logback-adapter ☭ sweat and blood
 * <p>
 * Base class for every logdoc appender, implements LogDoc protocol and parses raw log events for custom "fields"
 */
abstract class LogdocBase extends AppenderBase<ILoggingEvent> {
    /**
     * Running JVM instance local ID
     */
    protected static final String rtId = ManagementFactory.getRuntimeMXBean().getName();

    protected final ThrowableProxyConverter tpc = new ThrowableProxyConverter();

    /**
     * These are reserved field names for LogDoc service attributes, can't be used in custom fields
     */
    private static final Set<String> controlFields = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(TimeSrc, Pid, Source, Level, Message, TimeRcv, Ip, AppName)));

    /**
     * Mandatory config value -- LogDoc host address
     */
    protected String host;

    /**
     * Mandatory config value -- LogDoc host port
     */
    protected int port;

    /**
     * Optional config value -- static prefix for every log source
     */
    protected String prefix = "";

    /**
     * Optional config value -- static suffix for every log source
     */
    protected String suffix = "";

    /**
     * Mandatory config value -- application name, to track it in LogDoc by name
     */
    protected String appName;

    /**
     * Mandatory config value (defaults to false). Flag to map MDC to LogDoc "fields" if true.
     */
    protected boolean mapMdc;

    /* flaps */
    protected Sourcer sourcer = new SimpleSourcer();

    @Override
    public void start() {
        int errorCount = 0;

        if (port <= 0) {
            errorCount++;
            addError("No port was configured for appender " + name);
        }

        if (isEmpty(host)) {
            errorCount++;
            addError("No remote host was configured for appender " + name);
        }

        if (isEmpty(appName)) {
            errorCount++;
            addError("No application name was configured for appender " + name);
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

    /**
     * LogDoc log event is a sequence of 'name=value' pairs in a binary view (LE). Each event starts with a header [0x6, 0x3] and ends with a '\n' byte
     * Only one pair is mandatory - with name 'message' and nonempty value.
     * <p>
     * encode() converts logback log event into LogDoc log event.
     * <p>
     * Service fields are computed from event attributes, custom fields are parsed from event body by simple markup:
     * - If body contains '@@' substring - all after this mark is treated as fields
     * - One field it's a pair of name and value, separated by '=' char
     * - Field's pair end is a '\n' char or end of body
     * - Everything after '@@' substring, untill end of body and not matching to name/value pair is ignored
     *
     * @param event logback Log event
     * @return LogDoc log event in a binary view
     * @throws Exception if something went wrong
     */
    protected final byte[] encode(final ILoggingEvent event) throws Exception {
        final Map<String, String> fields = new HashMap<>(0);
        StringBuilder msg = new StringBuilder(notNull(event.getFormattedMessage()));
        final int sepIdx = msg.indexOf("@@");
        String rawFields = null;

        if (sepIdx != -1) {
            rawFields = msg.substring(sepIdx + 2).trim();

            if (rawFields.indexOf('=') != -1)
                msg.delete(sepIdx, msg.length());
            else
                rawFields = null;
        }

        if (event.getThrowableProxy() != null)
            msg.append("\n").append(tpc.convert(event));

        fields.put(Message, msg.toString());

        if (rawFields != null) {
            int sep, nxt;
            String pair;

            while ((sep = rawFields.indexOf('=')) != -1) {
                pair = (((nxt = rawFields.indexOf('@')) == -1) ? rawFields : rawFields.substring(0, nxt)).trim();

                final String n = pair.substring(0, sep).trim();
                final String v = pair.substring(sep + 1).trim();

                if (!n.isEmpty() && !v.isEmpty())
                    fields.put(n + (controlFields.contains(n) ? "_" : ""), v);

                rawFields = nxt == -1 ? "" : rawFields.substring(nxt + 1).trim();
            }
        }

        if (mapMdc && !isEmpty(event.getMDCPropertyMap()))
            event.getMDCPropertyMap().forEach((name, value) -> {
                if (!isEmpty(name))
                    fields.put(name + (controlFields.contains(name) ? "_" : ""), value);
            });

        fields.put(TimeSrc, Instant.ofEpochMilli(event.getTimeStamp()).atZone(ZoneId.systemDefault()).toLocalDateTime().format(logTimeFormat));
        fields.put(Pid, rtId);
        fields.put(Source, sourcer.apply(event.getLoggerName()));
        fields.put(Level, event.getLevel() == ch.qos.logback.classic.Level.TRACE ? "LOG" : event.getLevel().levelStr);
        fields.put(AppName, appName);
        fields.put("threadName", event.getThreadName());

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

    public String getAppName() {
        return appName;
    }

    public void setAppName(final String appName) {
        this.appName = isEmpty(appName) ? null : appName.replaceAll("[^a-zA-Z0-9-_]", "");
    }

    public boolean isMapMdc() {
        return mapMdc;
    }

    public void setMapMdc(final boolean mapMdc) {
        this.mapMdc = mapMdc;
    }
}
