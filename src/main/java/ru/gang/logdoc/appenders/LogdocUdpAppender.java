package ru.gang.logdoc.appenders;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:18
 * logback-adapter ☭ sweat and blood
 */
public class LogdocUdpAppender extends LogdocBase {
    private Future<?> task;
    private InetAddress address;
    private final AtomicReference<Consumer<Void>> sender = new AtomicReference<>(unused -> {});

    @Override
    protected boolean subStart() {
        try {
            address = InetAddress.getByName(host);
        } catch (final UnknownHostException ex) {
            addError("unknown host: " + host + ": " + ex.getMessage(), ex);
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
            try { task.cancel(true); } catch (final Exception ignore) { }
            task = getContext().getScheduledExecutorService().submit(this::doJob);
        }

        addInfo("смерть");
    }

    @Override
    protected DataOutputStream getDOS() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream(128);

        sender.set(unused -> {
            try {
                final DatagramSocket datagramSocket = new DatagramSocket();
                datagramSocket.setSoTimeout(15000);
                datagramSocket.send(new DatagramPacket(baos.toByteArray(), baos.size(), address, port));
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
