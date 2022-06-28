package org.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.ByteArrayOutputStream;
import java.net.*;
import java.util.Arrays;

import static org.logdoc.LogDocConstants.header;
import static org.logdoc.utils.Tools.token;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:18
 * logback-adapter ☭ sweat and blood
 */
public class LogdocUdpAppender extends LogdocBase {
    private InetAddress address;
    private DatagramSocket datagramSocket = null;
    private static final int SPLITLEN = 2028; // 2048 - 2 bytes header - 16 bytes token - 1 byte sequence size - 1 byte sequence part order

    @Override
    protected boolean subStart() {
        try {
            address = InetAddress.getByName(host);
            datagramSocket = new DatagramSocket();
            datagramSocket.setSoTimeout(15000);
        } catch (final UnknownHostException ex) {
            addError("unknown host: " + host + ": " + ex.getMessage(), ex);
            return false;
        } catch (final SocketException e) {
            addError("Не могу инициализировать сокет отправки: " + e.getMessage(), e);
            return false;
        }

        return true;
    }

    @Override
    protected void append(final ILoggingEvent event) {
        try {
            final byte[] data = encode(event);

            final int cycles = data.length / SPLITLEN + (data.length % SPLITLEN != 0 ? 1 : 0);
            final byte[] token = token();

            for (int i = 0; i < cycles; i++)
                try (final ByteArrayOutputStream pos = new ByteArrayOutputStream(2048)) {
                    pos.write(header);
                    pos.write((byte) cycles);
                    pos.write((byte) i);
                    pos.write(token);
                    pos.write(Arrays.copyOfRange(data, i * SPLITLEN, Math.min(data.length, (i + 1) * SPLITLEN)));
                    final byte[] part = pos.toByteArray();
                    datagramSocket.send(new DatagramPacket(part, 0, part.length, address, port));
                }
        } catch (final Exception e) {
            addError(e.getMessage(), e);
        }
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
