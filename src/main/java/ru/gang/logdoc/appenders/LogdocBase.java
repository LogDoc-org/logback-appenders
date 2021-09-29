package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ru.gang.logdoc.LogDoc;
import ru.gang.logdoc.flaps.Leveler;
import ru.gang.logdoc.flaps.Sourcer;
import ru.gang.logdoc.flaps.Timer;
import ru.gang.logdoc.flaps.impl.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import static ru.gang.logdoc.utils.Tools.isEmpty;
import static ru.gang.logdoc.utils.Tools.notNull;


/**
 * @author Denis Danilin | denis@danilin.name
 * 25.02.2021 12:40
 * logback-adapter ☭ sweat and blood
 */
abstract class LogdocBase extends AppenderBase<ILoggingEvent> {
    protected static final String rtId = ManagementFactory.getRuntimeMXBean().getName();

    protected final ThrowableProxyConverter tpc = new ThrowableProxyConverter();
    protected BlockingDeque<ILoggingEvent> deque;

    protected String host, prefix = "", suffix = "";
    protected int port, queueSize = 128;

    /* flaps */
    protected Sourcer sourcer = new SimpleSourcer();
    protected Timer timer = new SimpleTimer();
    protected Leveler leveler = new SimpleLeveler();

    @Override
    public void start() {
        int errorCount = 0;

        if (port <= 0) {
            errorCount++;
            addError("No port was configured for appender " + name);
        }

        if (host == null) {
            errorCount++;
            addError("No remote host was configured for appender " + name);
        }

        if (queueSize == 0)
            addWarn("Should use a queue size of one to indicate synchronous processing");

        if (queueSize < 0) {
            errorCount++;
            addError("Queue size must be greater than zero");
        }

        if (!prefix.isEmpty() && !suffix.isEmpty())
            sourcer = new SourcerBoth(prefix, suffix);
        else if (!prefix.isEmpty())
            sourcer = new PreSourcer(prefix);
        else if (!suffix.isEmpty())
            sourcer = new PostSourcer(suffix);

        if (errorCount == 0 && subStart()) {
            deque = new LinkedBlockingDeque<>(Math.max(queueSize, 1));
            super.start();
        }
    }

    protected abstract boolean subStart();

    @Override
    protected final void append(final ILoggingEvent event) {
        if (event == null || !isStarted())
            return;

        if (!deque.offer(event))
            addWarn("Message queue is overflowed, last message is dropped");
    }

    @SuppressWarnings("ConstantConditions")
    protected void rollQueue() throws IOException, InterruptedException {
        ILoggingEvent event;
        StringBuilder msg;
        int sepIdx;
        final Map<String, String> fields = new HashMap<>(0);
        String rawFields = null;

        while ((event = deque.takeFirst()) != null) {
            try {
                fields.clear();
                msg = new StringBuilder(notNull(event.getFormattedMessage()));

                if ((sepIdx = msg.toString().indexOf('\r')) != -1) {
                    rawFields = msg.substring(sepIdx + 1);

                    if (rawFields.indexOf('=') != -1)
                        msg.delete(sepIdx, msg.length());
                    else
                        rawFields = null;
                }

                if (event.getThrowableProxy() != null)
                    msg.append("\n").append(tpc.convert(event));

                fields.put(LogDoc.FieldMessage, msg.toString());

                if (rawFields != null)
                    Arrays.stream(rawFields.split("\r"))
                            .filter(p -> p.indexOf('=') != -1)
                            .forEach(p -> {
                                final String[] parts = p.split("=");

                                final String fName = notNull(parts[0]).replaceAll("[^a-zA-Z0-9_]", "");

                                if (isEmpty(fName) || fName.charAt(0) == '_' || Character.isDigit(fName.charAt(0)))
                                    return;

                                fields.put(LogDoc.controls.contains(fName) ? fName + "_" : fName, parts[1]);
                            });

                writeMsg(event, fields, getDOS());
                rolledCycle();
            } catch (Exception e) {
                addError(e.getMessage(), e);
                if (!deque.offerFirst(event))
                    addInfo("Не можем положить событие обратно в очередь - она заполнена.");

                throw e;
            }
        }

    }

    protected abstract DataOutputStream getDOS();

    protected abstract void rolledCycle() throws IOException;

    private void writeMsg(final ILoggingEvent event, final Map<String, String> fields, final DataOutputStream daos) throws IOException {
        writePair(LogDoc.FieldTimeStamp, timer.apply(event.getTimeStamp()), daos);
        writePair(LogDoc.FieldProcessId, rtId, daos);
        writePair(LogDoc.FieldSource, sourcer.apply(event.getLoggerName()), daos);
        writePair(LogDoc.FieldLevel, leveler.apply(event.getLevel()), daos);
        for (final Map.Entry<String, String> entry : fields.entrySet())
            writePair(entry.getKey(), entry.getValue(), daos);
        daos.write('\n');
    }

    private void writePair(final String key, final String value, final DataOutputStream daos) throws IOException {
        if (value.indexOf('\n') != -1)
            writeComplexPair(key, value, daos);
        else
            writeSimplePart(key, value, daos);
    }

    private void writeComplexPair(final String key, final String value, final DataOutputStream daos) throws IOException {
        final byte[] v = value.getBytes(StandardCharsets.UTF_8);
        daos.write(key.getBytes(StandardCharsets.UTF_8));
        daos.write('\n');
        daos.writeLong(v.length);
        daos.write(v);
    }

    private void writeSimplePart(final String key, final String value, final DataOutputStream daos) throws IOException {
        daos.write((key + "=" + value + "\n").getBytes(StandardCharsets.UTF_8));
        daos.write('=');
        daos.write(value.getBytes(StandardCharsets.UTF_8));
        daos.write('\n');
    }
}
