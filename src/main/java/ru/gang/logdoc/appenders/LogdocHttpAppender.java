package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.gang.logdoc.utils.Httper;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:44
 * logback-adapter ☭ sweat and blood
 */
public class LogdocHttpAppender extends LogdocBase {
    private final AtomicInteger failsCounter = new AtomicInteger(0);
    private final Queue<ILoggingEvent> queue = new ArrayDeque<>(256);
    private final AtomicBoolean httperRun = new AtomicBoolean(false);

    private boolean ssl, ignoreCACheck;
    private int timeout;
    private Httper httper;
    private URL url;

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

        return true;
    }

    @Override
    protected void append(final ILoggingEvent event) {
        queue.offer(event);

        if (httperRun.compareAndSet(false, true))
            CompletableFuture.runAsync(() -> {
                try {
                    final int code = httper.execute(url, () -> {
                        try (final ByteArrayOutputStream os = new ByteArrayOutputStream(4096)) {
                            ILoggingEvent e;
                            while ((e = queue.poll()) != null)
                                os.write(encode(e));

                            httperRun.set(false);

                            return os.toByteArray();
                        } catch (final Exception e) {
                            addError(e.getMessage(), e);
                        }

                        return new byte[0];
                    });
                    if (code != 200) {
                        if (failsCounter.incrementAndGet() >= 5000) {
                            addError("Maximum number of errors reached, appender shut down.");
                            stop();
                        } else
                            addWarn("Wrong logdoc response code: " + code);
                    } else
                        failsCounter.set(0);
                } catch (final Exception e) {
                    addError(e.getMessage(), e);
                }
            });
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
        return super.getHost();
    }

    public void setHost(final String host) {
        super.setHost(host);
    }

    public String getPrefix() {
        return super.getPrefix();
    }

    public void setPrefix(final String prefix) {
        super.setPrefix(prefix);
    }

    public String getSuffix() {
        return super.getSuffix();
    }

    public void setSuffix(final String suffix) {
        super.setSuffix(suffix);
    }

    public int getPort() {
        return super.getPort();
    }

    public void setPort(final int port) {
        super.setPort(port);
    }

    @Override
    public String getAppName() {
        return super.getAppName();
    }

    @Override
    public void setAppName(final String appName) {
        super.setAppName(appName);
    }

    @Override
    public boolean isMapMdc() {
        return super.isMapMdc();
    }

    @Override
    public void setMapMdc(final boolean mapMdc) {
        super.setMapMdc(mapMdc);
    }
}
