package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.02.2021 18:03
 * logback-adapter ☭ sweat and blood
 */
@SuppressWarnings("unused")
public class LogdocTcpAppender extends LogdocBase {

    private static final int SOCKET_CHECK_TIMEOUT = 5000;

    private Socket socket;
    private OutputStream daos;
    private final AtomicInteger connectFails = new AtomicInteger(0);
    private final AtomicBoolean onGuard = new AtomicBoolean(false);

    @Override
    protected boolean subStart() {
        if (!socketFails())
            addWarn("Connection is not ready, will try 50 times more later");

        return true;
    }

    @Override
    protected void append(final ILoggingEvent event) {
        if (onGuard.get())
            return;

        if (socketFails()) {
            if (!onGuard.compareAndSet(false, true))
                return;

            if (connectFails.incrementAndGet() >= 50) {
                addError("50 connect fails - we're done.");
                stop();
                return;
            } else {
                addWarn("Connect fail, going to repeat in " + (connectFails.get() * 5) + " seconds.");
                getContext().getScheduledExecutorService().schedule(() -> onGuard.set(false), connectFails.get() * 5L, TimeUnit.SECONDS);
            }

            return;
        }

        try {
            daos.write(encode(event));
            daos.flush();
        } catch (final Exception e) {
            addError(e.getMessage(), e);
        }
    }

    private boolean socketFails() {
        try {
            if (socket != null && socket.getInetAddress().isReachable(SOCKET_CHECK_TIMEOUT) && socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown())
                return false;

            socket = new Socket(InetAddress.getByName(host), port);
            daos = null;

            if (socket.isConnected()) {
                daos = new BufferedOutputStream(socket.getOutputStream());

                connectFails.set(0);
                return false;
            }
        } catch (final Exception e) {
            addError(e.getMessage(), e);
        }

        return true;
    }

    @Override
    public void stop() {
        if (!isStarted())
            return;
        try {daos.close();} catch (final Exception ignore) {}
        try {socket.close();} catch (final Exception ignore) {}

        super.stop();
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
}
