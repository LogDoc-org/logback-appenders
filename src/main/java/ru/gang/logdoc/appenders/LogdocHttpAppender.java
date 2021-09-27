package ru.gang.logdoc.appenders;

import ru.gang.logdoc.utils.Httper;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:44
 * logback-adapter ☭ sweat and blood
 */
public class LogdocHttpAppender extends LogdocBase {
    private boolean ssl, ignoreCACheck;
    private int timeout;
    private Httper httper;
    private URL url;

    private Future<?> task;
    private final AtomicReference<Consumer<Void>> sender = new AtomicReference<>(unused -> {});

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
            rollQueue();
        } catch (InterruptedException ignore) {
        } catch (final Exception e) {
            addError("Ошибка цикла отправки: " + e.getMessage(), e);
            addInfo("Уходим на рестарт");
            try {task.cancel(true);} catch (final Exception ignore) {}
            task = getContext().getScheduledExecutorService().submit(this::doJob);
        }

        addInfo("смерть");
    }

    @Override
    protected DataOutputStream getDOS() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

        sender.set(unused -> {
            try {
                httper.execute(url, Collections.emptyMap(),
                        os -> {
                            try {
                                os.write(baos.toByteArray());
                                os.flush();
                            } catch (IOException e) {
                                addError(e.getMessage(), e);
                            }
                        }, null);
            } catch (Exception e) {
                addError(e.getMessage(), e);
            } finally {
                try {baos.close();} catch (final Exception ignore) {}
            }
        });

        return new DataOutputStream(baos);
    }

    @Override
    protected void rolledCycle() {
        sender.get().accept(null);
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

}
