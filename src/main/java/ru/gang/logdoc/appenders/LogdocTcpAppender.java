package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Denis Danilin | denis@danilin.name
 * 24.02.2021 18:03
 * logback-adapter ☭ sweat and blood
 */
@SuppressWarnings("unused")
public class LogdocTcpAppender extends LogdocBase {

    private static final int SOCKET_CHECK_TIMEOUT = 5000;

    //    private Socket socket;
    private volatile OutputStream os = null;
    //    private final AtomicInteger connectFails = new AtomicInteger(0);
    private final AtomicBoolean armed = new AtomicBoolean(false);

    private final BlockingQueue<byte[]> overexposure = new ArrayBlockingQueue<>(50);

    @Override
    protected boolean subStart() {
        doConnect();

        return true;
    }

    @Override
    protected void append(final ILoggingEvent event) {

        final byte[] data;
        try {
            data = encode(event);
        } catch (final Exception e) {
            addWarn("Cant encode envent: " + e.getMessage(), e);
            return;
        }

        if (os == null) {
            while (!overexposure.offer(data)) {
                overexposure.poll();
                addWarn("Queue is full, removing oldest event");
            }

            doConnect();
            return;
        }

        try {
            os.write(encode(event));
            os.flush();
        } catch (final Exception e) {
            addWarn("Failed send event: " + e.getMessage());
            doConnect();
        }
    }

    private void doConnect() {
        if (!armed.compareAndSet(false, true))
            return;

        os = null;
        getContext().getScheduledExecutorService().schedule(this::doSocket, 5L, TimeUnit.SECONDS);
    }

    private void doSocket() {
        armed.set(false);

        try {
            os = new Socket(InetAddress.getByName(host), port).getOutputStream();

            byte[] toSend;

            while ((toSend = overexposure.poll()) != null)
                os.write(toSend);

            os.flush();
        } catch (final Exception e) {
            addError("Cant connect and flush queue: " + e.getMessage());
            doConnect();
        }
    }

    @Override
    public void stop() {
        if (!isStarted())
            return;
        try {os.close();} catch (final Exception ignore) {}

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
