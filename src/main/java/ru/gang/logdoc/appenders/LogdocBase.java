package ru.gang.logdoc.appenders;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ru.gang.logdoc.flaps.Leveler;
import ru.gang.logdoc.flaps.Sourcer;
import ru.gang.logdoc.flaps.Timer;
import ru.gang.logdoc.flaps.impl.*;
import ru.gang.logdoc.utils.LogDoc;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static ru.gang.logdoc.structs.utils.Tools.isEmpty;

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
        final AtomicReference<String> key = new AtomicReference<>(), value = new AtomicReference<>();
        int sepIdx;
        final Map<String, String> fields = new HashMap<>(0);
        while ((event = deque.takeFirst()) != null) {
            try {
                fields.clear();
                msg = new StringBuilder(event.getFormattedMessage());

                if ((sepIdx = msg.toString().indexOf(LogDoc.EndOfMessage)) != -1) {
                    final byte[] fld = msg.substring(sepIdx + 1).getBytes(StandardCharsets.UTF_8);
                    msg.delete(sepIdx, msg.length());
                    key.set("");
                    Consumer<Byte> filler = b -> key.updateAndGet(v -> v + (char) b.byteValue());

                    for (int i = 0; i < fld.length; i++) {
                        if (fld[i] == LogDoc.Equal) {
                            filler = b -> value.updateAndGet(v -> v + (char) b.byteValue());
                            continue;
                        } else if (fld[i] == LogDoc.EndOfMessage) {
                            if (i > 0 && fld[i - 1] != LogDoc.Escape) {
                                if (!isEmpty(key.get()) && !isEmpty(value.get()))
                                    fields.put(LogDoc.controls.contains(key.get()) ? key.get() + "_" : key.get(), value.get());

                                filler = b -> key.updateAndGet(v -> v + (char) b.byteValue());
                            }

                            continue;
                        }

                        filler.accept(fld[i]);
                    }
                }

                if (event.getThrowableProxy() != null)
                    msg.append("\n").append(tpc.convert(event));

                writeMsg(msg.toString(), event, fields, getDOS());
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

    private void writeMsg(final String part, final ILoggingEvent event, final Map<String, String> fields, final DataOutputStream daos) throws IOException {
        daos.write((byte) LogDoc.NextPacket);
        writePair(LogDoc.FieldTimeStamp, timer.apply(event.getTimeStamp()), daos);
        writePair(LogDoc.FieldProcessId, rtId, daos);
        writePair(LogDoc.FieldSource, sourcer.apply(event.getLoggerName()), daos);
        writePair(LogDoc.FieldLevel, leveler.apply(event.getLevel()), daos);
        writePair(LogDoc.FieldMessage, part, daos);
        for (final Map.Entry<String, String> entry : fields.entrySet())
            writePair(entry.getKey(), entry.getValue(), daos);
    }

    private void writePair(final String key, final String value, final DataOutputStream daos) throws IOException {
        if (value.indexOf(LogDoc.EndOfMessage) != -1)
            writeComplexPair(key, value, daos);
        else
            writeSimplePart(key, value, daos);
    }

    private void writeComplexPair(final String key, final String value, final DataOutputStream daos) throws IOException {
        final byte[] v = value.getBytes(StandardCharsets.UTF_8);
        daos.write(key.getBytes(StandardCharsets.UTF_8));
        daos.writeLong(v.length);
        daos.write(v);
        daos.write(LogDoc.EndOfMessage);
    }

    private void writeSimplePart(final String key, final String value, final DataOutputStream daos) throws IOException {
        daos.write(key.getBytes(StandardCharsets.UTF_8));
        daos.write('=');
        daos.write(value.getBytes(StandardCharsets.UTF_8));
        daos.write(LogDoc.EndOfMessage);
    }
}
