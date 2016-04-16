/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mephi.var.data;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.net.StandardSocketOptions;
import java.util.Map.Entry;

/**
 *
 * @author Роман
 */
public class DataServer {

    private static Logger logger = Logger.getLogger(DataServer.class.getName());

    public void read(String serverIP) {
        
        logger.setLevel(Level.INFO);

        final Storage store = new Storage();

        final ConcurrentHashMap<String, AsynchronousSocketChannel> accessMainSockets
                = new ConcurrentHashMap<String, AsynchronousSocketChannel>();
        final ConcurrentHashMap<String, AsynchronousSocketChannel> accessInfoSockets
                = new ConcurrentHashMap<String, AsynchronousSocketChannel>();

        try {
            final AsynchronousServerSocketChannel serverSocketChannel = AsynchronousServerSocketChannel
                    .open(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));
            final AsynchronousServerSocketChannel serverInfoSocketChannel = AsynchronousServerSocketChannel
                    .open(AsynchronousChannelGroup.withFixedThreadPool(10, Executors.defaultThreadFactory()));
            serverSocketChannel.bind(new InetSocketAddress(serverIP, 1455));
            serverInfoSocketChannel.bind(new InetSocketAddress(serverIP, 1456));

            serverInfoSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {

                @Override
                public void completed(AsynchronousSocketChannel socket, Void attachment) {
                    // Question: Зачем это??
                    serverInfoSocketChannel.accept(null, this);

                    // Question: Зачем?
                    try {
                        socket.setOption(StandardSocketOptions.SO_RCVBUF, 13107200);
                        socket.setOption(StandardSocketOptions.SO_SNDBUF, 13107200);
                    } catch (IOException e1) {
                        // TODO Написать обработку для catch-блока
                        e1.printStackTrace();
                    }

                    int index;
                    String toSplit = null;
                    try {
                        index = socket.getRemoteAddress().toString().lastIndexOf(":");

                        toSplit = socket.getRemoteAddress().toString().substring(1, index);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (toSplit != null) {
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

                    try {
                        clientSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 13107200);
                        clientSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 13107200);
                    } catch (IOException e1) {
                        // TODO Написать обработку для catch-блока
                        e1.printStackTrace();
                    }

                    int index;
                    String toSplit = null;
                    try {
                        index = clientSocketChannel.getRemoteAddress().toString().lastIndexOf(":");

                        toSplit = clientSocketChannel.getRemoteAddress().toString().substring(1, index);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (toSplit != null) {
                        accessMainSockets.put(toSplit, clientSocketChannel);

                        logger.log(Level.INFO, "Got income main connection " + toSplit);
                    }

                    // блокирующий стиль
                    try {
                        // Question: Почему не просто allocate?
                        ByteBuffer buf = ByteBuffer.allocateDirect(24);
                        //ByteBuffer responseBufferInfo = ByteBuffer.allocateDirect(13);
                        //System.out.println("Access connected");
                        while (true) {
                            // Question: зачем get? для блокировки, пока не закончится операция?
                            // Сообщение 24 байта: |ip|length|UUID| ??
                            //                      4    4     16
                            clientSocketChannel.read(buf).get();
                            //logger.log(Level.INFO, "Got income message");

                            // Question: надо проверить, действительно ли указатель стоит не на начале, так как get на
                            // предыдущем шаге вызывается у Future, а не у buf
                            buf.rewind();

                            ByteBuffer ip = ByteBuffer.allocateDirect(4);
                            // Question: тоже, зачем, если при аллокате указатель на 0 ставится
                            ip.rewind();
                            ip.put(buf.get()); //ip
                            ip.put(buf.get());
                            ip.put(buf.get());
                            ip.put(buf.get());
                            ip.rewind();

                            // String clientIp1 = new String(String.valueOf(ipOne1) + "." +
                            // String.valueOf(ipTwo1) + "." + String.valueOf(ipThree1) + "." +
                            // String.valueOf(ipFour1));
                            int length = buf.getInt();

                            // Question: зачем, когда можно getLong без параметров?
                            int pos = buf.position();
                            UUID reqId;
                            reqId = new UUID(buf.getLong(pos), buf.getLong(pos + 8));

                            ByteBuffer data = ByteBuffer.allocateDirect(length);
                            // Question: зачем get? для блокировки, пока не закончится операция?
                            // data: |  |   |opType|UUID|data
                            //        1   1     3    16
                            clientSocketChannel.read(data).get();
                            data.rewind();

                            byte[] response;
                            response = msgProcessor(data, store, ip, reqId);
                            ByteBuffer responseBufferInfo = ByteBuffer.wrap(response);

                            responseBufferInfo.rewind();

                            clientSocketChannel.write(responseBufferInfo).get();

                            buf.clear();
                            responseBufferInfo.rewind();
                            data.rewind();

                            // Question: data.postion() на предыдущем шаге установлена в 0. Зачем вызывать функцию еще раз?
                            int opType = data.get(data.position() + 2) & 0xff;
                            int isOk = responseBufferInfo.get() & 0xff;
                            responseBufferInfo.clear();
                            UUID key = new UUID(data.getLong(data.position() + 5), data.getLong(data.position() + 5 + 8));

                            //logger.log(Level.INFO, "Got income UUID " + key);
                            data.rewind();

                            ByteBuffer infoAccessBuf = ByteBuffer.allocateDirect(18);

                            // Question: зачем здесь бинарные операции опять?
                            infoAccessBuf.put((byte) (opType & 0xff));
                            infoAccessBuf.put((byte) (isOk & 0xff));
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
                        Logger.getLogger(DataServer.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(DataServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Обработчик сообщений от сервера доступа
     *
     * @param msg данные
     * @param store хранилище данных
     * @param ip ip
     * @param reqId ключ запроса
     * @return готовый к отправке буфур с результатом операции
     */
    public byte[] msgProcessor(ByteBuffer msg, Storage store, ByteBuffer ip, UUID reqId) {
        msg.rewind();
        msg.get();
        msg.get();

        int opType = msg.get() & 0xff; // 1 - DELETE, 2 - PUT, 3 - GET
        int pos;


        msg.get();// //пропуск OBJECT_ID
        // Question: почему пропуск UUID делается считыванием только одного байта?
        msg.get();// //пропуск UUID (видимо все таки OBJ_ID type)
        pos = msg.position();
        UUID uuid;
        uuid = new UUID(msg.getLong(pos), msg.getLong(pos + 8));

        // System.out.println("Type of operation " + opType);
        byte isOk;
        ByteBuffer response;
        switch (opType) {
            case 1:
                // DELETE
                if (store.delete(uuid) != false) {
                	logger.fine("DELETE:success");
                    isOk = 1;
                } else {
                	logger.fine("DELETE:fail");
                    isOk = 0;
                }
                response = ByteBuffer.allocate(46);
                response.rewind();
                response.put((byte) (isOk & 0xff)); //байт успешности
                response.put(ip);                   //ip
                response.putLong(reqId.getMostSignificantBits()); //reqId
                response.putLong(reqId.getLeastSignificantBits());
                // Question: можно подробнее про id запроса, его ключ и зачем нужны все данные ниже?
                response.putInt(21);                //длина
                response.put((byte) (1 & 0xff)); //request
                response.put((byte) (1 & 0xff)); //byte
                response.put((byte) (opType & 0xff)); //req_type
                response.put((byte) (2 & 0xff)); //object_id
                response.put((byte) (2 & 0xff)); //uuid
                response.putLong(uuid.getMostSignificantBits()); //ключ
                response.putLong(uuid.getLeastSignificantBits());
                response.rewind();
                break;
            case 2:
                // PUT
                msg.position(msg.position() + 16); // пропуск ключа
                msg.get();// //пропуск OBJECT
                byte type = msg.get();// //считываем INT,LONG...
                int dataLength = msg.remaining(); // длина "чистых" данных
                byte[] data = new byte[msg.remaining()];
                msg.get(data);
                Data dataForStorage = new Data(data, type, dataLength);
                if (store.put(uuid, dataForStorage) != false) {
                    logger.fine("PUT:success");
                    isOk = 1;
                } else {
                    logger.fine("PUT:fail");
                    isOk = 0;
                }
                response = ByteBuffer.allocate(46);
                response.rewind();
                response.put((byte) (isOk & 0xff)); //байт успешности
                response.put(ip);                   //ip
                response.putLong(reqId.getMostSignificantBits()); //reqId
                response.putLong(reqId.getLeastSignificantBits());
                response.putInt(21);                //длина
                response.put((byte) (1 & 0xff)); //request
                response.put((byte) (1 & 0xff)); //byte
                response.put((byte) (opType & 0xff)); //req_type
                response.put((byte) (2 & 0xff)); //object_id
                response.put((byte) (2 & 0xff)); //uuid
                response.putLong(uuid.getMostSignificantBits()); //ключ
                response.putLong(uuid.getLeastSignificantBits());
                response.rewind();
                break;
            case 3:
                // GET
                Data dataFromStorage = new Data();
                if ((dataFromStorage = (store.get(uuid))) != null) {
                    isOk = 1;
                    response = ByteBuffer.allocate(25 + 21 + 2 + dataFromStorage.getLength());
                } else {
                    isOk = 0;
                    response = ByteBuffer.allocate(25 + 21);
                }
                response.rewind();
                response.put((byte) (isOk & 0xff)); //байт успешности
                response.put(ip);                   //ip
                response.putLong(reqId.getMostSignificantBits()); //reqId
                response.putLong(reqId.getLeastSignificantBits());
                if (isOk == 1) {
                    response.putInt(21 + 2 + dataFromStorage.getLength());//длина
                } else {
                    response.putInt(21);
                }
                response.put((byte) (1 & 0xff)); //request
                response.put((byte) (1 & 0xff)); //byte
                response.put((byte) (opType & 0xff)); //req_type
                response.put((byte) (2 & 0xff)); //object_id
                response.put((byte) (2 & 0xff)); //uuid
                response.putLong(uuid.getMostSignificantBits()); //ключ
                response.putLong(uuid.getLeastSignificantBits());
                if (isOk == 1) {
                    response.put((byte) (3 & 0xff)); //object
                    response.put((byte) (dataFromStorage.getType() & 0xff)); //тип
                    response.put(dataFromStorage.getData());
                }
                response.rewind();
                break;
            default:
                response = ByteBuffer.allocate(0);

        }
        return response.array();
    }

}
