package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ru.gang.logdoc.structs.enums.BinMsg;
import ru.gang.logdoc.utils.Httper;

import java.io.*;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.zip.GZIPOutputStream;

/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 16:44
 * logback-adapter ☭ sweat and blood
 */
public class LogdocHttpAppender extends LogdocBase {
    private static final String CmdHeader = "ldCmd", TokenHeader = "ldToken";
    private boolean ssl, ignoreCACheck;
    private int timeout;
    private Httper httper;
    private URL url;

    private Future<?> task;

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
            while (true) {
                final ILoggingEvent event = deque.takeFirst();
                final Map<String, String> headers = new HashMap<>(0);

                try {
                    if (tokenBytes.length < 16) {
                        headers.put(CmdHeader, BinMsg.AppendersRequestToken.name());

                        httper.execute(url, headers,
                                os -> {
                                    try {
                                        askToken(new DataOutputStream(os));
                                        os.flush();
                                    } catch (IOException e) {
                                        throw new RuntimeException("Cant get token");
                                    }
                                },
                                is -> {
                                    try {
                                        readToken(new DataInputStream(is));
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                        deque.offerFirst(event);
                        continue;
                    }

                    headers.put(CmdHeader, BinMsg.LogEvent.name());
                    headers.put(TokenHeader, token);

                    final String msg = event.getFormattedMessage();
                    final Map<String, String> fields = fielder.apply(msg);

                    for (final String part : multiplexer.apply(cleaner.apply(msg) + (event.getThrowableProxy() != null ? "\n" + tpc.convert(event) : "")))
                        httper.execute(url, headers,
                                os -> {
                                    try (final DataOutputStream daos = new DataOutputStream(os)) {
                                        writePart(part, event, fields, daos);
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }, null);

                } catch (Exception e) {
                    if (!deque.offerFirst(event))
                        addInfo("Dropping event due to socket connection error and maxed out deque capacity");
                }
            }
        } catch (InterruptedException ignore) {
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
}
