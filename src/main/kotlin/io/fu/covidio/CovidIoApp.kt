package io.fu.covidio

import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.ChannelPipeline
import io.netty.channel.EventLoopGroup
import io.netty.channel.ServerChannel
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpServerCodec
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolConfig
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketServerCompressionHandler
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate
import io.netty.handler.stream.ChunkedWriteHandler

const val WS_PATH = "/ws"

fun main(args: Array<String>) {
    // TODO
    start(1, 8080)
}

fun start(realmCount: Int, port: Int) {
    val allocator = allocator()
    val sslContext = initSslContext()

    val lobby = initLobby(
        realmCount = realmCount,
        encoder = GameEncoder(allocator),
        decoder = GameDecoder()
    )

    val channelInitializer = chInit(
        wsHandler = WsHandler(lobby),
        sslContext = sslContext
    )
    val master = eventLoopGroup(1)
    val slave = eventLoopGroup(4)
    val bootstrap = servBootstrap(
        masterLoopGroup = master,
        slaveLoopGroup = slave,
        allocator = allocator,
        childHandler = channelInitializer
    )

    try {
        println("Starting websocket application on port=$port and path=$WS_PATH")
        val channel = bootstrap.bind(port).channel()
        channel.closeFuture().sync()
    } catch (e: Exception) {
        throw RuntimeException("Failed to start server", e)
    } finally {
        slave.shutdownGracefully()
        master.shutdownGracefully()
    }
}

fun socketChannelType(): Class<out ServerChannel> =
    if (Epoll.isAvailable())
        EpollServerSocketChannel::class.java
    else
        KQueueServerSocketChannel::class.java

fun eventLoopGroup(nThreads: Int): EventLoopGroup =
    if (Epoll.isAvailable())
        EpollEventLoopGroup(nThreads)
    else
        KQueueEventLoopGroup(nThreads)

private fun allocator(): ByteBufAllocator =
    PooledByteBufAllocator(true)

private fun servBootstrap(
    masterLoopGroup: EventLoopGroup,
    slaveLoopGroup: EventLoopGroup,
    allocator: ByteBufAllocator,
    childHandler: ChannelInitializer<Channel>
): ServerBootstrap =
    ServerBootstrap()
        .channel(socketChannelType())
        .group(masterLoopGroup, slaveLoopGroup)
        .option(ChannelOption.SO_BACKLOG, 1024)
        .option(ChannelOption.SO_REUSEADDR, true)
        .childOption(ChannelOption.ALLOCATOR, allocator)
        .childOption(ChannelOption.SO_REUSEADDR, true)
        .childHandler(childHandler)

private fun chInit(
    wsHandler: WsHandler,
    sslContext: SslContext?
): ChannelInitializer<Channel> =
    object : ChannelInitializer<Channel>() {
        override fun initChannel(ch: Channel) {
            val pipeline: ChannelPipeline = ch.pipeline()

            sslContext?.also {
                pipeline.addLast(it.newHandler(ch.alloc()))
            }

            pipeline
                .addLast(HttpServerCodec())
                .addLast(ChunkedWriteHandler())
                .addLast(HttpObjectAggregator(65536))
                .addLast(WebSocketServerCompressionHandler())
                .addLast(
                    WebSocketServerProtocolHandler(
                        WebSocketServerProtocolConfig.newBuilder()
                            .allowExtensions(true)
                            .checkStartsWith(true)
                            .websocketPath(WS_PATH)
                            .build()
                    )
                )
                .addLast(wsHandler)
        }
    }

private fun initSslContext(): SslContext? {
    return System.getProperty("ssl")?.let {
        return try {
            val ssc = SelfSignedCertificate()
            SslContextBuilder.forServer(
                ssc.certificate(),
                ssc.privateKey()
            ).build()
        } catch (e: Exception) {
            throw RuntimeException("Failed to init ssl context", e)
        }
    }
}
