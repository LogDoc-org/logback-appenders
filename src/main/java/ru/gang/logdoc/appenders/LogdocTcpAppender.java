package ru.gang.logdoc.appenders;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.02.2021 18:03
 * logback-adapter ☭ sweat and blood
 */
@SuppressWarnings("unused")
public class LogdocTcpAppender extends LogdocBase {

    private static final int SOCKET_CHECK_TIMEOUT = 5000;

    private String peerId;
    private Consumer<Void> closer = unused -> {
    };
    private Socket socket;
    private DataOutputStream daos;
    private DataInputStream dais;
    private Future<?> task;

    private final Random r = new Random();
    private final byte[] partialId = new byte[8];

    @Override
    protected boolean subStart() {
        closer = unused -> {
            try {
                dais.close();
            } catch (final Exception ignore) {
            }
            try {
                daos.close();
            } catch (final Exception ignore) {
            }
            try {
                socket.close();
            } catch (final Exception ignore) {
            }
            socket = null;
        };

        peerId = host + ":" + port + ": ";
        addInfo("Наш хост: " + peerId);

        task = getContext().getScheduledExecutorService().submit(this::mainEndlessCycle);

        return true;
    }

    private void mainEndlessCycle() {
        addInfo("Главный цикл аппендера (пере-)запущен");

        try {
            if (socketFails()) {
                addWarn("Коннект не готов, главный цикл остановлен, ждём 3 сек.");
                try { task.cancel(true); } catch (final Exception ignore) { }
                try { closer.accept(null); } catch (final Exception ignore) { }
                task = getContext().getScheduledExecutorService().schedule(this::mainEndlessCycle, 3, TimeUnit.SECONDS);
                return;
            }

            rollQueue();
            daos.flush();
        } catch (final InterruptedException e0) {
            closer.accept(null);
            addInfo(peerId + "коннект закрыт - нас прервали, логгер умер.");
        } catch (final Exception e) {
            addError(peerId + "Ошибка цикла отправки: " + e.getMessage(), e);
            addInfo("Уходим на рестарт");
            try { task.cancel(true); } catch (final Exception ignore) { }
            try { closer.accept(null); } catch (final Exception ignore) { }
            task = getContext().getScheduledExecutorService().schedule(this::mainEndlessCycle, 5, TimeUnit.SECONDS);
        }

        addInfo("Главный цикл аппендера остановлен");
    }

    @Override
    protected DataOutputStream getDOS() {
        return daos;
    }

    @Override
    protected void rolledCycle() throws IOException {
        if (deque.isEmpty())
            daos.flush();
    }

    private boolean socketFails() {
        try {
            if (socket != null && socket.getInetAddress().isReachable(SOCKET_CHECK_TIMEOUT) && socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown())
                return false;

            socket = new Socket(InetAddress.getByName(host), port);
            daos = null;
            dais = null;

            if (socket.isConnected()) {
                dais = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                daos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                return false;
            }
        } catch (final Exception e) {
            addError(peerId + e.getMessage(), e);
            closer.accept(null);
        }

        return true;
    }

    @Override
    public void stop() {
        if (!isStarted())
            return;
        closer.accept(null);
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
