package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.net.DefaultSocketConnector;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.02.2021 18:03
 * logback-adapter ☭ sweat and blood
 */
public class LogdocTcpAppender extends LogdocBase {
    private static final int THRESHOLD = 4;
    private static final String stringLenPattern = "(?<=\\G.{" + THRESHOLD + "})";
    private String peerId;
    private Consumer<Void> closer = unused -> {
    };
    private Callable<?> connector;
    private Socket socket;
    private DataOutputStream daos;
    private DataInputStream dais;
    private Future<?> task;

    private final byte[] partialId = new byte[8];
    private final Random r = new Random();

    @Override
    protected boolean subStart() {
        try {
            connector = new DefaultSocketConnector(InetAddress.getByName(host), port, 0, retryDelay);

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

                            String[] eventSplitted = event.getMessage().split(stringLenPattern);
                            if (eventSplitted.length > 1) {

                                /*for (int eventSplittedCnt = 0; eventSplittedCnt < eventSplitted.length; eventSplittedCnt++) {
                                    r.nextBytes(partialId);
                                    writePartCompose(eventSplitted[eventSplittedCnt], eventSplitted.length,
                                            eventSplittedCnt == 0 ? 0 : (THRESHOLD * eventSplittedCnt) - 1, partialId,
                                            event, fields, daos);
                                }*/

                                // TODO Убрать потом, демо отправки лога по частям по 4 символа в разном порядке
                                int eventSplittedCnt = 2;
                                r.nextBytes(partialId);
                                writePartCompose(eventSplitted[eventSplittedCnt], eventSplitted.length,
                                        (THRESHOLD * eventSplittedCnt) - 1, partialId,
                                        event, fields, daos);

                                eventSplittedCnt = 0;
                                writePartCompose(eventSplitted[eventSplittedCnt], eventSplitted.length,
                                        0, partialId,
                                        event, fields, daos);

                                eventSplittedCnt = 3;
                                writePartCompose(eventSplitted[eventSplittedCnt], eventSplitted.length,
                                        (THRESHOLD * eventSplittedCnt) - 1, partialId,
                                        event, fields, daos);

                                eventSplittedCnt = 1;
                                writePartCompose(eventSplitted[eventSplittedCnt], eventSplitted.length,
                                        (THRESHOLD * eventSplittedCnt) - 1, partialId,
                                        event, fields, daos);

                            } else
                                for (final String part : multiplexer.apply(cleaner.apply(msg) + (event.getThrowableProxy() != null ? "\n" + tpc.convert(event) : "")))
                                    writePart(part, event, fields, daos);

                            if (deque.isEmpty())
                                daos.flush();
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
                dais = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                daos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

                if (tokenBytes.length < 15) {
                    askToken(daos);
                    daos.flush();

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
}
