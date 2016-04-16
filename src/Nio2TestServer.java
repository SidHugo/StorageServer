/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.StandardSocketOptions;

/**
 * @author Кирилл
 */
public class Nio2TestServer {

    private static Logger logger = Logger.getLogger(Nio2TestServer.class.getName());

    public void read() {
        
        logger.setLevel(Level.SEVERE);

        final ConcurrentHashMap<String, AsynchronousSocketChannel> accessMainSockets =
                new ConcurrentHashMap<String, AsynchronousSocketChannel>();
        final ConcurrentHashMap<String, AsynchronousSocketChannel> accessInfoSockets =
                new ConcurrentHashMap<String, AsynchronousSocketChannel>();

        try {
            final AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel
                    .open(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));
            final AsynchronousServerSocketChannel serverInfoSocketChannel = AsynchronousServerSocketChannel
                    .open(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));
            serverSocketChannel.bind(new InetSocketAddress("127.0.0.1", 1455));
            serverInfoSocketChannel.bind(new InetSocketAddress("127.0.0.1", 1456));

            serverInfoSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

                @Override
                public void completed(AsynchronousSocketChannel socket, Void attachment) {
                    
                    serverInfoSocketChannel.accept(null, this);
                    
                    int index;
                    String toSplit = null;
                    try {
                        index = socket.getRemoteAddress().toString().lastIndexOf(":");

                        toSplit = socket.getRemoteAddress().toString().substring(1, index);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(toSplit != null) {
                        accessInfoSockets.put(toSplit, socket);

                        logger.log(Level.INFO, "Got income info connection " + toSplit);
                    }

                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    // TODO Написать тело метода

                }
            });

            serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

                @Override
                public void completed(AsynchronousSocketChannel clientSocketChannel, Void attachment) {

                    serverSocketChannel.accept(null, this);
                    
                    int index;
                    String toSplit = null;
                    try {
                        index = clientSocketChannel.getRemoteAddress().toString().lastIndexOf(":");

                        toSplit = clientSocketChannel.getRemoteAddress().toString().substring(1, index);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(toSplit != null) {
                        accessMainSockets.put(toSplit, clientSocketChannel);

                        logger.log(Level.INFO, "Got income main connection " + toSplit);
                    }

                    // блокирующий стиль
                    try {
                        ByteBuffer buf1 = ByteBuffer.allocateDirect(4 + 4 + 16);
                        ByteBuffer responseBufferInfo = ByteBuffer.allocateDirect(46);
                        
                        while(true) {
                            clientSocketChannel.read(buf1).get();
                            logger.info("Got income message");

                            buf1.rewind();

                            int clientId = buf1.getInt();

                            int length1 = buf1.getInt();
                            
                            UUID requestId = new UUID(buf1.getLong(buf1.position()), buf1.getLong(buf1.position() + 8));

                            ByteBuffer data = ByteBuffer.allocateDirect(length1);
                            clientSocketChannel.read(data).get();
                            
                            data.rewind();
                            
                            int requestType = data.get(data.position() + 2) & 0xff;
                            UUID key = new UUID(data.getLong(data.position() + 5), data.getLong(data.position() + 13));

                            data.rewind();

                            logger.info("Request: key:" + key +
                                        ", length:" + length1 + 
                                        ", request ID: " + requestId +
                                        ", request type:" + requestType);
                            
                            responseBufferInfo.put((byte) (1 & 0xff));

                            responseBufferInfo.putInt(clientId);
                            
                            responseBufferInfo.putLong(requestId.getMostSignificantBits());
                            responseBufferInfo.putLong(requestId.getLeastSignificantBits());
                            
                            responseBufferInfo.putInt(21);
							responseBufferInfo.put((byte) (1 & 0xff));//request
							responseBufferInfo.put((byte) (1 & 0xff));//BYTE
							responseBufferInfo.put((byte) (requestType & 0xff));//PUT

							responseBufferInfo.put((byte) (3 & 0xff));//object_id
							responseBufferInfo.put((byte) (7 & 0xff));//UUID
                            responseBufferInfo.putLong(key.getMostSignificantBits());
                            responseBufferInfo.putLong(key.getLeastSignificantBits());
                            
                            responseBufferInfo.rewind();

                            clientSocketChannel.write(responseBufferInfo).get();

                            responseBufferInfo.clear();
                            buf1.clear();
                            
                            int opType = data.get(data.position() + 2) & 0xff;
                            
                            key = new UUID(data.getLong(data.position() + 5), data.getLong(data.position() + 5 + 8));

                            
                            
                            data.rewind();
                            
                            ByteBuffer infoAccessBuf = ByteBuffer.allocateDirect(18);
                            
                            infoAccessBuf.put((byte) (opType & 0xff));
                            infoAccessBuf.put((byte) (1 & 0xff));
                            infoAccessBuf.putLong(key.getMostSignificantBits());
                            infoAccessBuf.putLong(key.getLeastSignificantBits());
                            infoAccessBuf.rewind();



                            for (Entry<String, AsynchronousSocketChannel> entry : accessInfoSockets.entrySet()) {
                                AsynchronousSocketChannel toWrite = entry.getValue();
                                synchronized (toWrite) {
                                    toWrite.write(infoAccessBuf).get();
                                    
                                }
                            }

                        }

                    } catch (Exception ex) {
                        Logger.getLogger(Nio2TestServer.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    throw new UnsupportedOperationException("Not supported yet."); // To change body
                                                                                   // of generated
                                                                                   // methods,
                                                                                   // choose Tools |
                                                                                   // Templates.
                }
            });
            System.out.println("Server ready to accept connections");
            System.in.read();
        } catch (IOException ex) {
            Logger.getLogger(Nio2TestServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        Nio2TestServer client = new Nio2TestServer();
        client.read();
    }
}
