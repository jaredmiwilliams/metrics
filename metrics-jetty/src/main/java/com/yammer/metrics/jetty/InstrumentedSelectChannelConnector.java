package com.yammer.metrics.jetty;

import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Counter;
import com.yammer.metrics.core.Meter;
import com.yammer.metrics.core.MetricsRegistry;
import com.yammer.metrics.core.Timer;

import org.eclipse.jetty.http.HttpHeaders;
import org.eclipse.jetty.io.AsyncEndPoint;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.Connection;
import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.io.nio.AsyncConnection;
import org.eclipse.jetty.server.AsyncHttpConnection;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.TimeUnit;

public class InstrumentedSelectChannelConnector extends SelectChannelConnector {
    private final Timer duration, durationWithoutKeepAlive;
    private final Meter accepts, connects, disconnects;
    private final Counter connections;

    public InstrumentedSelectChannelConnector(int port) {
        this(Metrics.defaultRegistry(), port);
    }

    public InstrumentedSelectChannelConnector(MetricsRegistry registry,
                                              int port) {
        super();
        setPort(port);
        this.duration = registry.newTimer(SelectChannelConnector.class,
                                          "connection-duration",
                                          Integer.toString(port),
                                          TimeUnit.MILLISECONDS,
                                          TimeUnit.SECONDS);
        this.durationWithoutKeepAlive = registry.newTimer(SelectChannelConnector.class,
                                          "connection-duration-without-keep-alive",
                                          Integer.toString(port),
                                          TimeUnit.MILLISECONDS,
                                          TimeUnit.SECONDS);
        this.accepts = registry.newMeter(SelectChannelConnector.class,
                                         "accepts",
                                         Integer.toString(port),
                                         "connections",
                                         TimeUnit.SECONDS);
        this.connects = registry.newMeter(SelectChannelConnector.class,
                                          "connects",
                                          Integer.toString(port),
                                          "connections",
                                          TimeUnit.SECONDS);
        this.disconnects = registry.newMeter(SelectChannelConnector.class,
                                             "disconnects",
                                             Integer.toString(port),
                                             "connections",
                                             TimeUnit.SECONDS);
        this.connections = registry.newCounter(SelectChannelConnector.class,
                                               "active-connections",
                                               Integer.toString(port));
    }

    @Override
    public void accept(int acceptorID) throws IOException {
        super.accept(acceptorID);
        accepts.mark();
    }

    @Override
    protected AsyncConnection newConnection(SocketChannel channel, final AsyncEndPoint endpoint) {
        return new KeepAliveAwareAsyncHttpConnection(this, endpoint, getServer());
    }

    @Override
    protected void connectionOpened(Connection connection) {
        connections.inc();
        super.connectionOpened(connection);
        connects.mark();
    }

    @Override
    protected void connectionClosed(Connection connection) {
        super.connectionClosed(connection);
        disconnects.mark();
        final long duration = System.currentTimeMillis() - connection.getTimeStamp();
        this.duration.update(duration, TimeUnit.MILLISECONDS);
        if (connection instanceof KeepAliveAwareAsyncHttpConnection
            && !((KeepAliveAwareAsyncHttpConnection) connection).isKeepAlive()) {
            this.durationWithoutKeepAlive.update(duration, TimeUnit.MILLISECONDS);
        }
        connections.dec();
    }

    public class KeepAliveAwareAsyncHttpConnection extends AsyncHttpConnection {

        boolean isKeepAlive = false;

        public KeepAliveAwareAsyncHttpConnection(Connector connector, EndPoint endpoint, Server server) {
            super(connector, endpoint, server);
        }

        @Override
        protected void parsedHeader(Buffer name, Buffer value) throws IOException {
            super.parsedHeader(name, value);

            if (!isKeepAlive
                && HttpHeaders.CACHE.getOrdinal(name) == HttpHeaders.CONNECTION_ORDINAL
                && HttpHeaders.CACHE.getOrdinal(value) == HttpHeaders.KEEP_ALIVE_ORDINAL) {
                isKeepAlive = true;
            }
        }

        public boolean isKeepAlive() {
            return isKeepAlive;
        }

    }

}
