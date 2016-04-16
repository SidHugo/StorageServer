/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.mephi.var.data;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
import gost28147.Gost28147;

/**
 * Класс хранилища данных
 *
 * @author Роман
 */
public class Storage {

    private final ConcurrentHashMap<UUID, Data> store = new ConcurrentHashMap<>();

    public Storage() {
    }

    /**
     * Операция PUT
     *
     * @param id ключ
     * @param data данные
     * @return код успешности операции
     */
    public boolean put(UUID id, Data data) {
        Gost28147 gost = new Gost28147();
        ByteBuffer bb;
        bb = ByteBuffer.allocate(16);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        byte[] arrId;
        arrId = bb.array();
        byte[] encryptedId;
        encryptedId = gost.gammaEncrypt(arrId);
        bb = ByteBuffer.wrap(encryptedId);
        UUID encryptedUuid = new UUID(bb.getLong(), bb.getLong());
        if (!store.containsKey(encryptedUuid)) {
            byte[] encryptedData;
            encryptedData = gost.gammaEncrypt(data.getData());
            Data dataForStore = new Data(encryptedData, data.getType(), data.getLength());
            store.put(encryptedUuid, dataForStore);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Операция PUT
     *
     * @param id ключ
     * @return данные
     */
    public Data get(UUID id) {
        Gost28147 gost = new Gost28147();
        ByteBuffer bb;
        bb = ByteBuffer.allocate(16);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        byte[] arrId;
        arrId = bb.array();
        byte[] encryptedId;
        encryptedId = gost.gammaEncrypt(arrId);
        bb = ByteBuffer.wrap(encryptedId);
        UUID encryptedUuid = new UUID(bb.getLong(), bb.getLong());
        if (store.containsKey(encryptedUuid)) {
            byte[] decryptedData;
            decryptedData = gost.gammaDecrypt((store.get(encryptedUuid)).getData());
            Data dataForReturn = new Data(decryptedData, (store.get(encryptedUuid)).getType(), (store.get(encryptedUuid)).getLength());
            return dataForReturn;
        } else {
            return null;
        }
    }

    /**
     * Операция DELETE
     *
     * @param id ключ
     * @return код успешности операции
     */
    public boolean delete(UUID id) {
        Gost28147 gost = new Gost28147();
        ByteBuffer bb;
        bb = ByteBuffer.allocate(16);
        bb.putLong(id.getMostSignificantBits());
        bb.putLong(id.getLeastSignificantBits());
        byte[] arrId;
        arrId = bb.array();
        byte[] encryptedId;
        encryptedId = gost.gammaEncrypt(arrId);
        bb = ByteBuffer.wrap(encryptedId);
        UUID encryptedUuid = new UUID(bb.getLong(), bb.getLong());
        if (store.containsKey(encryptedUuid)) {
            store.remove(encryptedUuid);
            return true;
        } else {
            return false;
        }
    }
}
