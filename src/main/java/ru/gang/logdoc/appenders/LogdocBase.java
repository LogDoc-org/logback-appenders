package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.util.Duration;
import ru.gang.logdoc.flaps.*;
import ru.gang.logdoc.flaps.Timer;
import ru.gang.logdoc.flaps.impl.*;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.Field;
import ru.gang.logdoc.model.StaticPosFields;
import ru.gang.logdoc.structs.enums.BinMsg;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:40
 * logback-adapter ☭ sweat and blood
 */
abstract class LogdocBase extends AppenderBase<ILoggingEvent> {
    protected static final Duration eventDelayLimit = new Duration(100);
    protected static final String rtId = ManagementFactory.getRuntimeMXBean().getName();
    protected static final byte[] header = new byte[]{(byte) 6, (byte) 3};

    protected final ThrowableProxyConverter tpc = new ThrowableProxyConverter();
    protected BlockingDeque<ILoggingEvent> deque;

    protected String host, login, password, token, prefix = "", suffix = "";
    protected int port, queueSize = 128, retryDelay = 30000;
    protected boolean skipTime = false, skipSource = false, skipLevel = false, multiline = true;
    protected byte[] tokenBytes = new byte[0];
    protected StaticPosFields staticPrefix = null, staticSuffix = null;
    protected DynamicPosFields dynamicFields = null;
    protected final SortedSet<String> sortedNames = new TreeSet<>();

    /* flaps */
    protected Sourcer sourcer = new SimpleSourcer();
    protected Timer timer = new SimpleTimer();
    protected Leveler leveler = new SimpleLeveler();
    protected Multiplexer multiplexer = new EmptyMultiplexer();
    protected Fielder fielder = new EmptyFielder();
    protected Cleaner cleaner = new EmptyCleaner();

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

        if (queueSize == 0)
            addWarn("Should use a queue size of one to indicate synchronous processing");

        if (queueSize < 0) {
            errorCount++;
            addError("Queue size must be greater than zero");
        }

        if (errorCount == 0) {
            if (staticPrefix != null)
                sortedNames.addAll(staticPrefix.getFields().stream().filter(Objects::nonNull).map(Field::getFieldName).collect(Collectors.toList()));

            if (staticSuffix != null)
                sortedNames.addAll(staticSuffix.getFields().stream().filter(Objects::nonNull).map(Field::getFieldName).collect(Collectors.toList()));

            if (dynamicFields != null)
                sortedNames.addAll(dynamicFields.getFields().stream().filter(Objects::nonNull).map(Field::getFieldName).collect(Collectors.toList()));
        }

        if (token != null && !token.trim().isEmpty()) {
            final UUID uuid = UUID.fromString(token);
            final ByteBuffer bb = ByteBuffer.wrap(new byte[16]);

            bb.putLong(uuid.getMostSignificantBits());
            bb.putLong(uuid.getLeastSignificantBits());

            tokenBytes = bb.array();
        }

        if (skipSource)
            sourcer = new EmptySourcer();
        else {
            if (!prefix.isEmpty() && !suffix.isEmpty())
                sourcer = new SourcerBoth(prefix, suffix);
            else if (!prefix.isEmpty())
                sourcer = new PreSourcer(prefix);
            else if (!suffix.isEmpty())
                sourcer = new PostSourcer(suffix);
        }

        if (skipTime)
            timer = new EmptyTimer();

        if (skipLevel)
            leveler = new EmptyLeveler();

        if (!multiline)
            multiplexer = new SimpleMultiplexer();

        if (!sortedNames.isEmpty()) {
            final boolean pre = staticPrefix != null && !staticPrefix.getFields().isEmpty();
            final boolean post = staticSuffix != null && !staticSuffix.getFields().isEmpty();
            final boolean dyn = dynamicFields != null && !dynamicFields.getFields().isEmpty();

            if (pre && post && dyn)
                fielder = new PreDynPostFielder(staticPrefix, dynamicFields, staticSuffix);
            else if (pre && dyn)
                fielder = new PreDynFielder(staticPrefix, dynamicFields);
            else if (pre && post)
                fielder = new PrePostFielder(staticPrefix, staticSuffix);
            else if (dyn && post)
                fielder = new DynPostFielder(dynamicFields, staticSuffix);
            else if (pre)
                fielder = new PreFielder(staticPrefix);
            else if (dyn)
                fielder = new DynFielder(dynamicFields);
            else if (post)
                fielder = new PostFielder(staticSuffix);

            final boolean cpre = pre && staticPrefix.getClear();
            final boolean cdyn = dyn && (dynamicFields.isClearValues() || dynamicFields.isClearMarks());
            final boolean cpost = post && staticSuffix.getClear();

            if (cpre && cdyn && cpost)
                cleaner = new PreDynPostCleaner(staticPrefix, dynamicFields, staticSuffix);
            else if (cpre && cdyn)
                cleaner = new PreDynCleaner(staticPrefix, dynamicFields);
            else if (cpre && cpost)
                cleaner = new PrePostCleaner(staticPrefix, staticSuffix);
            else if (cdyn && cpost)
                cleaner = new DynPostCleaner(dynamicFields, staticSuffix);
            else if (cpre)
                cleaner = new PreCleaner(staticPrefix);
            else if (cdyn)
                cleaner = new DynCleaner(dynamicFields);
            else if (cpost)
                cleaner = new PostCleaner(staticSuffix);
        }

        if (errorCount == 0 && subStart()) {
            deque = new LinkedBlockingDeque<>(Math.max(queueSize, 1));
            super.start();
        }
    }

    protected abstract boolean subStart();

    @Override
    protected final void append(final ILoggingEvent event) {
        if (event == null || !isStarted())
            return;

        try {
            if (!deque.offer(event, eventDelayLimit.getMilliseconds(), TimeUnit.MILLISECONDS))
                addInfo("Dropping event due to timeout limit of [" + eventDelayLimit + "] being exceeded");
        } catch (InterruptedException e) {
            addError("Interrupted while appending event to SocketAppender", e);
        }
    }

    protected void readToken(final DataInputStream dais) throws IOException {
        final int response = dais.readByte();
        if (response == 0) {
            tokenBytes = new byte[16];

            if (dais.read(tokenBytes) != 16)
                throw new IOException("Cant read/interpretate server response with token");

            final ByteBuffer bb = ByteBuffer.wrap(tokenBytes);

            token = new UUID(bb.getLong(), bb.getLong()).toString();
        } else {
            String error = "Unknow error on server side";
            try {
                error = dais.readUTF();
            } catch (final Exception ignore) {
            }
            throw new IOException(error);
        }
    }

    protected void askToken(final DataOutputStream daos) throws IOException {
        daos.write(header);
        daos.writeByte(BinMsg.AppendersRequestToken.ordinal());
        daos.writeUTF(login == null ? "" : login);
        daos.writeUTF(password == null ? "" : password);
        daos.writeBoolean(skipTime);
        daos.writeBoolean(skipSource);
        daos.writeBoolean(skipLevel);
        daos.writeBoolean(multiline);
        daos.writeShort(sortedNames.size());
        for (final String fieldName : sortedNames)
            daos.writeUTF(fieldName);
    }

    protected void askConfig(final DataOutputStream daos) throws IOException {
        daos.write(header);
        daos.writeByte(BinMsg.AppendersRequestConfig.ordinal());
        daos.write(tokenBytes);
    }

    protected void writePart(final String part, final ILoggingEvent event, final Map<String, String> fields, final DataOutputStream daos) throws IOException {
        daos.write(header);
        daos.writeByte(BinMsg.LogEvent.ordinal());
        daos.write(tokenBytes);
        daos.writeUTF(timer.apply(event.getTimeStamp()));
        daos.writeUTF(rtId);
        daos.writeUTF(sourcer.apply(event.getLoggerName()));
        daos.writeByte(leveler.apply(event.getLevel()));
        daos.writeByte(0); // partial count
        daos.writeByte(0); // partial index
        daos.writeUTF(part);
        daos.writeShort(sortedNames.size());
        for (final String n : sortedNames)
            daos.writeUTF(fields.getOrDefault(n, ""));
    }

    public final String getHost() {
        return host;
    }

    public final void setHost(final String host) {
        this.host = host;
    }

    public final String getLogin() {
        return login;
    }

    public final void setLogin(final String login) {
        this.login = login;
    }

    public final String getPassword() {
        return password;
    }

    public final void setPassword(final String password) {
        this.password = password;
    }

    public final String getToken() {
        return token;
    }

    public final void setToken(final String token) {
        this.token = token;
    }

    public final String getPrefix() {
        return prefix;
    }

    public final void setPrefix(final String prefix) {
        this.prefix = prefix == null ? "" : prefix.trim() + (prefix.trim().endsWith(".") ? "" : ".");
    }

    public final String getSuffix() {
        return suffix;
    }

    public final void setSuffix(final String suffix) {
        this.suffix = suffix == null ? "" : (suffix.trim().startsWith(".") ? "" : ".") + suffix.trim();
    }

    public final int getPort() {
        return port;
    }

    public final void setPort(final int port) {
        this.port = port;
    }

    public final int getQueueSize() {
        return queueSize;
    }

    public final void setQueueSize(final int queueSize) {
        this.queueSize = queueSize;
    }

    public final int getRetryDelay() {
        return retryDelay;
    }

    public final void setRetryDelay(final int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public final boolean isSkipTime() {
        return skipTime;
    }

    public final void setSkipTime(final boolean skipTime) {
        this.skipTime = skipTime;
    }

    public final boolean isSkipSource() {
        return skipSource;
    }

    public final void setSkipSource(final boolean skipSource) {
        this.skipSource = skipSource;
    }

    public final boolean isSkipLevel() {
        return skipLevel;
    }

    public final void setSkipLevel(final boolean skipLevel) {
        this.skipLevel = skipLevel;
    }

    public final boolean isMultiline() {
        return multiline;
    }

    public final void setMultiline(final boolean multiline) {
        this.multiline = multiline;
    }

    public final byte[] getTokenBytes() {
        return tokenBytes;
    }

    public final void setTokenBytes(final byte[] tokenBytes) {
        this.tokenBytes = tokenBytes;
    }

    public final StaticPosFields getStaticPrefix() {
        return staticPrefix;
    }

    public final void setStaticPrefix(final StaticPosFields staticPrefix) {
        this.staticPrefix = staticPrefix;
    }

    public final StaticPosFields getStaticSuffix() {
        return staticSuffix;
    }

    public final void setStaticSuffix(final StaticPosFields staticSuffix) {
        this.staticSuffix = staticSuffix;
    }

    public final DynamicPosFields getDynamicFields() {
        return dynamicFields;
    }

    public final void setDynamicFields(final DynamicPosFields dynamicFields) {
        this.dynamicFields = dynamicFields;
    }
}
