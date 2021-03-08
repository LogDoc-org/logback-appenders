package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.gang.logdoc.structs.enums.BinMsg;
import ru.gang.logdoc.structs.utils.Tools;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

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

                            if (buffer.get() != header[0] || buffer.get() != header[1] || buffer.get() != BinMsg.NettyTokenResponse.ordinal())
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
                    if (!deque.offerFirst(event))
                        addInfo("Dropping event due to socket connection error and maxed out deque capacity");
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException ignore) {
            ignore.printStackTrace();
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


}
