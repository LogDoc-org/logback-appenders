package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.gang.logdoc.model.DynamicPosFields;
import ru.gang.logdoc.model.StaticPosFields;
import ru.gang.logdoc.protocol.AppProto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:18
 * logback-adapter ☭ sweat and blood
 */
public class LogdocUdpAppender extends LogdocBase {
    private Future<?> task;
    private InetAddress address;

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
            while (true) {
                final ILoggingEvent event = deque.takeFirst();

                try {
                    final DatagramSocket datagramSocket = new DatagramSocket();
                    datagramSocket.setSoTimeout(15000);

                    if (tokenBytes.length < 16)
                        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(128); final DataOutputStream daos = new DataOutputStream(baos)) {
                            askToken(daos);
                            daos.flush();

                            datagramSocket.send(new DatagramPacket(baos.toByteArray(), baos.size(), address, port));

                            final DatagramPacket reply = new DatagramPacket(new byte[1024], 1024);
                            datagramSocket.receive(reply);
                            final ByteBuffer buffer = ByteBuffer.wrap(reply.getData());

                            if (buffer.get() != header[0] || buffer.get() != header[1] || buffer.get() != AppProto.NettyTokenResponse.ordinal())
                                throw new IOException("Wrong header");

                            tokenBytes = new byte[16];
                            buffer.get(tokenBytes);
                        }

                    final String msg = event.getFormattedMessage();
                    final Map<String, String> fields = fielder.apply(msg);

                    for (final String part : multiplexer.apply(cleaner.apply(msg) + (event.getThrowableProxy() != null ? "\n" + tpc.convert(event) : "")))
                        try (final ByteArrayOutputStream baos = new ByteArrayOutputStream(128); final DataOutputStream daos = new DataOutputStream(baos)) {
                            writePart(part, event, fields, daos);
                            datagramSocket.send(new DatagramPacket(baos.toByteArray(), baos.size(), address, port));
                        }

                } catch (Exception e) {
                    addError(e.getMessage(), e);
                    if (!deque.offerFirst(event))
                        addInfo("Dropping event due to socket connection error and maxed out deque capacity");
                }
            }
        } catch (InterruptedException ignore) {
        }

        addInfo("смерть");
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
        this.stringTokenSize = stringTokenSize != null ? stringTokenSize : -1;
    }

}
