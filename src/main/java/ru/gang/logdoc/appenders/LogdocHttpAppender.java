package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;
import ru.gang.logdoc.protocol.AppProto;
import ru.gang.logdoc.utils.Httper;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:44
 * logback-adapter ☭ sweat and blood
 */
public class LogdocHttpAppender extends LogdocBase {
    private static final String CmdHeader = "ldCmd", TokenHeader = "ldToken";
    private boolean ssl, ignoreCACheck;
    private int timeout;
    private Httper httper;
    private URL url;

    private Future<?> task;

    @Override
    @SuppressWarnings("ResultOfMethodCallIgnored")
    protected boolean subStart() {
        try {
            InetAddress.getByName(host);

            url = new URL((ssl ? "https://" : "http://") + host + ":" + port + "/");

            httper = new Httper(!ignoreCACheck, timeout > 0 ? timeout * 1000L : 15000);
        } catch (final UnknownHostException | MalformedURLException ex) {
            addError("unknown host: " + host);
            return false;
        }

        task = getContext().getScheduledExecutorService().submit(this::doJob);

        return true;
    }

    private void doJob() {
        try {
            while (true) {
                final ILoggingEvent event = deque.takeFirst();
                final Map<String, String> headers = new HashMap<>(0);

                try {
                    if (tokenBytes.length < 16) {
                        headers.put(CmdHeader, AppProto.AppendersRequestToken.name());

                        httper.execute(url, headers,
                                os -> {
                                    try {
                                        askToken(new DataOutputStream(os));
                                        os.flush();
                                        addInfo("Спросили токен");
                                    } catch (IOException e) {
                                        throw new RuntimeException("Cant get token");
                                    }
                                },
                                is -> {
                                    try {
                                        readToken(new DataInputStream(is));
                                        addInfo("Прочитали токен: " + token);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                        deque.offerFirst(event);
                        continue;
                    }

                    headers.put(CmdHeader, AppProto.LogEvent.name());
                    headers.put(TokenHeader, token);

                    final String msg = event.getFormattedMessage();
                    final Map<String, String> fields = fielder.apply(msg);

                    addInfo("Отправляем событие: " + msg);
                    for (final String part : multiplexer.apply(cleaner.apply(msg) + (event.getThrowableProxy() != null ? "\n" + tpc.convert(event) : "")))
                        httper.execute(url, headers,
                                os -> {
                                    try (final DataOutputStream daos = new DataOutputStream(os)) {
                                        writePart(part, event, fields, daos);
                                        addInfo("\tзаписали часть: " + part);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, null);

                } catch (Exception e) {
                    addError(e.getMessage(), e);
                    if (!deque.offerFirst(event))
                        addInfo("Dropping event due to socket connection error and maxed out deque capacity");
                }
            }
        } catch (InterruptedException ignore) {
        }

        addInfo("shutting down");
    }

    @Override
    public void stop() {
        if (!isStarted())
            return;

        task.cancel(true);
        super.stop();
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(final boolean ssl) {
        this.ssl = ssl;
    }

    public boolean isIgnoreCACheck() {
        return ignoreCACheck;
    }

    public void setIgnoreCACheck(final boolean ignoreCACheck) {
        this.ignoreCACheck = ignoreCACheck;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(final int timeout) {
        this.timeout = timeout;
    }

    public String getHost() {
        return host;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(final String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(final String token) {
        this.token = token;
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

    public int getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(final int queueSize) {
        this.queueSize = queueSize;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(final int retryDelay) {
        this.retryDelay = retryDelay;
    }

    public boolean isSkipTime() {
        return skipTime;
    }

    public void setSkipTime(final boolean skipTime) {
        this.skipTime = skipTime;
    }

    public boolean isSkipSource() {
        return skipSource;
    }

    public void setSkipSource(final boolean skipSource) {
        this.skipSource = skipSource;
    }

    public boolean isSkipLevel() {
        return skipLevel;
    }

    public void setSkipLevel(final boolean skipLevel) {
        this.skipLevel = skipLevel;
    }

    public boolean isMultiline() {
        return multiline;
    }

    public void setMultiline(final boolean multiline) {
        this.multiline = multiline;
    }

    public byte[] getTokenBytes() {
        return tokenBytes;
    }

    public void setTokenBytes(final byte[] tokenBytes) {
        this.tokenBytes = tokenBytes;
    }

    public StaticPosFields getStaticPrefix() {
        return staticPrefix;
    }

    public void setStaticPrefix(final StaticPosFields staticPrefix) {
        this.staticPrefix = staticPrefix;
    }

    public StaticPosFields getStaticSuffix() {
        return staticSuffix;
    }

    public void setStaticSuffix(final StaticPosFields staticSuffix) {
        this.staticSuffix = staticSuffix;
    }

    public DynamicPosFields getDynamicFields() {
        return dynamicFields;
    }

    public void setDynamicFields(final DynamicPosFields dynamicFields) {
        this.dynamicFields = dynamicFields;
    }

    public int getStringTokenSize() {
        return stringTokenSize;
    }

    public void setDynamicFields(final Integer stringTokenSize) {
        this.stringTokenSize = stringTokenSize != null ? stringTokenSize: -1;
    }

}
