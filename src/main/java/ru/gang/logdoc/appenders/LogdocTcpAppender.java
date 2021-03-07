package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.DefaultSocketConnector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.02.2021 18:03
 * logback-adapter ☭ sweat and blood
 */
public class LogdocTcpAppender extends LogdocBase {
    private String peerId;

    private Consumer<Void> closer = unused -> { };

    private Callable<?> connector;

    private Socket socket;

    private DataOutputStream daos;
    private DataInputStream dais;

    private Future<?> task;

    @Override
    protected boolean subStart() {
        try {
            connector = new DefaultSocketConnector(InetAddress.getByName(host), port, 0, retryDelay);

            closer = unused -> {
                try { dais.close(); } catch (final Exception ignore) { }
                try { daos.close(); } catch (final Exception ignore) { }
                try { socket.close(); } catch (final Exception ignore) { }
            };
        } catch (final UnknownHostException ex) {
            addError("unknown host: " + host);
            return false;
        }

        peerId = "remote peer " + host + ":" + port + ": ";
        task = getContext().getScheduledExecutorService().submit(this::doJob);

        return true;
    }

    private void doJob() {
        try {
            while (checkIfSocketOk()) {
                try {
                    addInfo(peerId + "connection established");

                    while (true) {
                        final ILoggingEvent event = deque.takeFirst();

                        try {
                            final String msg = event.getFormattedMessage();
                            final Map<String, String> fields = fielder.apply(msg);

                            for (final String part : multiplexer.apply(cleaner.apply(msg) + (event.getThrowableProxy() != null ? "\n" + tpc.convert(event) : "")))
                                writePart(part, event, fields, daos);
                        } catch (Exception e) {
                            if (!deque.offerFirst(event))
                                addInfo("Dropping event due to socket connection error and maxed out deque capacity");

                            throw e;
                        }
                    }
                } catch (IOException ex) {
                    addInfo(peerId + "connection failed: " + ex);
                } finally {
                    closer.accept(null);
                    addInfo(peerId + "connection closed");
                }
            }
        } catch (InterruptedException ignore) {
        }

        addInfo("shutting down");
    }

    private boolean checkIfSocketOk() {
        try {
            if (socket != null && socket.isConnected() && !socket.isOutputShutdown() && !socket.isInputShutdown())
                return true;

            socket = (Socket) connector.call();
            daos = null;
            dais = null;

            if (socket != null) {
                dais = new DataInputStream(socket.getInputStream());
                daos = new DataOutputStream(socket.getOutputStream());

                if (tokenBytes.length < 15) {
                    askToken(daos);

                    if (dais.readByte() != header[0] || dais.readByte() != header[1])
                        throw new IOException("Wrong header");

                    readToken(dais);
                }

                return true;
            }
        } catch (final Exception e) {
            addError(peerId + e.getMessage(), e);
            closer.accept(null);
        }

        return false;
    }

    @Override
    public void stop() {
        if (!isStarted())
            return;
        closer.accept(null);
        task.cancel(true);
        super.stop();
    }
}
